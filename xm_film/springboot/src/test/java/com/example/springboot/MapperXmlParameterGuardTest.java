package com.example.springboot;

import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapper XML 参数名守卫（类级）。
 *
 * <p>背景：{@code OrderedMapper.countPickedUpByUserAndFilm} 的 XML 曾写 {@code #{pickedUp}}，
 * 而方法签名只有 userId / filmId。Mockito 单测不碰 XML，403 条测试全绿，
 * 真库上却恒抛 BindingException（500）。此前只有这一条被专门补了加载测试。
 *
 * <p>本类做<b>静态</b>全量检查：把每个 {@code <select|insert|update|delete>} 里的
 * {@code #{...}} 根属性，与方法签名（@Param 名 / 单个 bean 属性 / foreach item）比对。
 * 不需要数据库，新增语句时写错参数名会直接变红。
 *
 * <p><b>覆盖边界</b>：{@code <sql>} 片段单独定义、被 {@code <include>} 引用的占位符
 * 不在此检查（片段没有方法签名可对照）；`${...}` 不检查；foreach 的 item 是动态变量，
 * 只认名字不认类型。
 */
class MapperXmlParameterGuardTest {

    private static final Pattern STATEMENT = Pattern.compile(
            "<(select|insert|update|delete)\\s+[^>]*?id=\"([^\"]+)\"[^>]*>(.*?)</\\1>",
            Pattern.DOTALL);
    private static final Pattern NAMESPACE = Pattern.compile("namespace=\"([^\"]+)\"");
    private static final Pattern PLACEHOLDER = Pattern.compile("#\\{\\s*([^,}\\s]+)");
    private static final Pattern FOREACH_ITEM = Pattern.compile("item=\"([^\"]+)\"");

    @Test
    void everyPlaceholderResolvesToAParameterOrBeanProperty() throws Exception {
        Path mapperDir = Path.of("src/main/resources/mapper");
        assertThat(mapperDir).as("mapper 目录应存在").isDirectory();

        List<String> errors = new ArrayList<>();
        try (Stream<Path> files = Files.list(mapperDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".xml"))
                    .sorted()
                    .forEach(file -> checkFile(file, errors));
        }

        assertThat(errors)
                .as("Mapper XML 里出现了方法签名/实体上不存在的参数名")
                .isEmpty();
    }

    private void checkFile(Path file, List<String> errors) {
        String xml;
        try {
            xml = Files.readString(file, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("读取 mapper 失败: " + file, e);
        }
        // 注释里的示例不参与检查
        xml = xml.replaceAll("(?s)<!--.*?-->", "");

        Matcher ns = NAMESPACE.matcher(xml);
        if (!ns.find()) {
            return;
        }
        Class<?> mapper;
        try {
            mapper = Class.forName(ns.group(1));
        } catch (ClassNotFoundException e) {
            errors.add(file.getFileName() + ": namespace 对应的接口不存在 " + ns.group(1));
            return;
        }

        Matcher statements = STATEMENT.matcher(xml);
        while (statements.find()) {
            String id = statements.group(2);
            String body = statements.group(3);
            List<Method> methods = methodsNamed(mapper, id);
            if (methods.isEmpty()) {
                errors.add(file.getFileName() + ": 语句 " + id + " 找不到同名 mapper 方法");
                continue;
            }

            Allowed allowed = new Allowed(entityType(mapper));
            for (Method method : methods) {
                allowed.merge(method);
            }
            Matcher foreach = FOREACH_ITEM.matcher(body);
            while (foreach.find()) {
                allowed.roots.add(foreach.group(1));
            }

            Matcher placeholder = PLACEHOLDER.matcher(body);
            while (placeholder.find()) {
                String token = placeholder.group(1);
                if (token.startsWith("__frch") || token.startsWith("ew.")) {
                    continue;
                }
                String root = token.contains(".") ? token.substring(0, token.indexOf('.')) : token;
                if (!allowed.allowAll && !allowed.roots.contains(root)) {
                    errors.add(file.getFileName() + ": " + id + " 的 #{" + token
                            + "} 找不到参数 " + root + "（可用: " + allowed.roots + "）");
                    continue;
                }
                if (token.contains(".") && allowed.types.containsKey(root)) {
                    if (!hasPath(allowed.types.get(root), token.substring(token.indexOf('.') + 1))) {
                        errors.add(file.getFileName() + ": " + id + " 的 #{" + token
                                + "} 在 " + allowed.types.get(root).getSimpleName() + " 上不存在");
                    }
                }
            }
        }
    }

    private static List<Method> methodsNamed(Class<?> mapper, String name) {
        List<Method> result = new ArrayList<>();
        for (Method method : mapper.getMethods()) {
            if (method.getName().equals(name)) {
                result.add(method);
            }
        }
        return result;
    }

    /** 解析 mapper 接口上的 BaseMapper&lt;Entity&gt; 实参，供继承来的 insert/selectAll 使用 */
    private static Class<?> entityType(Class<?> mapper) {
        for (java.lang.reflect.Type type : mapper.getGenericInterfaces()) {
            if (type instanceof java.lang.reflect.ParameterizedType parameterized
                    && parameterized.getRawType() == com.example.springboot.common.BaseMapper.class) {
                return (Class<?>) parameterized.getActualTypeArguments()[0];
            }
        }
        return null;
    }

    /** 收集一个方法允许的占位符根名，以及根名到类型的映射（供嵌套属性校验） */
    private static final class Allowed {
        final Set<String> roots = new HashSet<>();
        final Map<String, Class<?>> types = new HashMap<>();
        final Class<?> entityType;
        boolean allowAll;

        Allowed(Class<?> entityType) {
            this.entityType = entityType;
        }

        void merge(Method method) {
            Parameter[] parameters = method.getParameters();
            boolean anyParamAnnotation = false;
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Param param = parameter.getAnnotation(Param.class);
                if (param != null) {
                    anyParamAnnotation = true;
                    roots.add(param.value());
                    types.put(param.value(), parameter.getType());
                }
                roots.add("param" + (i + 1));
                roots.add("arg" + i);
                types.put("param" + (i + 1), parameter.getType());
                types.put("arg" + i, parameter.getType());
            }

            if (parameters.length == 1 && !anyParamAnnotation) {
                Class<?> type = parameters[0].getType();
                // BaseMapper<T> 的 T 无上界，擦除后是 Object；用接口实参补回实体类型
                if (entityType != null && (type == Object.class || type.isInterface())
                        && !Map.class.isAssignableFrom(type)) {
                    type = entityType;
                }
                if (isSimple(type) || Map.class.isAssignableFrom(type)) {
                    // MyBatis 对简单类型/Map 不校验属性名，这里也不做无意义拦截
                    allowAll = true;
                } else {
                    addBeanProperties(type);
                }
            }
        }

        private void addBeanProperties(Class<?> type) {
            for (Class<?> current = type; current != null && current != Object.class;
                 current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    roots.add(field.getName());
                    types.put(field.getName(), field.getType());
                }
                for (Method method : current.getDeclaredMethods()) {
                    String property = getterProperty(method);
                    if (property != null) {
                        roots.add(property);
                        types.put(property, method.getReturnType());
                    }
                }
            }
        }
    }

    /** 是否包含 a.b.c 这样的嵌套属性 */
    private static boolean hasPath(Class<?> type, String path) {
        Class<?> current = type;
        for (String segment : path.split("\\.")) {
            if (current == null || isSimple(current) || Map.class.isAssignableFrom(current)) {
                return true;
            }
            Class<?> next = propertyType(current, segment);
            if (next == null) {
                return false;
            }
            current = next;
        }
        return true;
    }

    private static Class<?> propertyType(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class;
             current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                return field.getType();
            } catch (NoSuchFieldException ignored) {
                // try getter
            }
            for (Method method : current.getDeclaredMethods()) {
                if (name.equals(getterProperty(method))) {
                    return method.getReturnType();
                }
            }
        }
        return null;
    }

    private static String getterProperty(Method method) {
        if (method.getParameterCount() != 0) {
            return null;
        }
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return null;
    }

    private static boolean isSimple(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || java.time.temporal.Temporal.class.isAssignableFrom(type)
                || java.util.Date.class.isAssignableFrom(type);
    }
}
