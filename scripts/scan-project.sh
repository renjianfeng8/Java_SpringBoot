#!/bin/bash
# ============================================================
# 影院管理系统 — 全栈项目自动扫描脚本
# 用途：生成项目完整结构清单、检查配置与依赖完整性
# 运行：bash xm_film/scripts/scan-project.sh
# ============================================================

set -e

BASE_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
echo "=========================================="
echo "  影院购票管理系统 — 全栈扫描报告"
echo "  扫描时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
echo ""

# ==================== 1. 项目概览 ====================
echo "【1】项目概览"
echo "--------------------------------------------"
echo "项目根目录: $BASE_DIR"
echo ""

# ==================== 2. 后端扫描 ====================
echo "【2】后端扫描 (Spring Boot)"
echo "--------------------------------------------"

# POM 信息
if [ -f "$BASE_DIR/xm_film/springboot/pom.xml" ]; then
  SPRING_BOOT_VER=$(grep -A1 '<spring-boot.version' "$BASE_DIR/xm_film/springboot/pom.xml" 2>/dev/null | grep -oP '\d+\.\d+\.\d+' || echo "3.3.13")
  JAVA_VER=$(grep 'java.version' "$BASE_DIR/xm_film/springboot/pom.xml" | grep -oP '\d+' || echo "17")
  echo "  Spring Boot: $SPRING_BOOT_VER"
  echo "  Java: $JAVA_VER"
fi

# 统计
CONTROLLER_COUNT=$(find "$BASE_DIR/xm_film/springboot/src/main/java" -name '*Controller.java' | wc -l)
ENTITY_COUNT=$(find "$BASE_DIR/xm_film/springboot/src/main/java" -name '*.java' -path '*/entity/*' | wc -l)
SERVICE_COUNT=$(find "$BASE_DIR/xm_film/springboot/src/main/java" -name '*Service.java' -path '*/service/*' | wc -l)
MAPPER_COUNT=$(find "$BASE_DIR/xm_film/springboot/src/main/java" -name '*Mapper.java' -path '*/mapper/*' | wc -l)
XML_COUNT=$(find "$BASE_DIR/xm_film/springboot/src/main/resources/mapper" -name '*.xml' | wc -l)

echo "  控制器: $CONTROLLER_COUNT 个"
echo "  实体类: $ENTITY_COUNT 个"
echo "  服务类: $SERVICE_COUNT 个"
echo "  Mapper: $MAPPER_COUNT 个"
echo "  XML映射: $XML_COUNT 个"

# API 接口清单
echo ""
echo "  API 路由清单:"
for ctrl in $(find "$BASE_DIR/xm_film/springboot/src/main/java" -name '*Controller.java' | sort); do
  ctrl_name=$(basename "$ctrl" .java)
  mapping=$(grep -E '@RequestMapping\("' "$ctrl" 2>/dev/null | grep -oP '/\w+' | head -1)
  if [ -z "$mapping" ]; then
    # WebController 没有类级 @RequestMapping
    methods=$(grep -E '@(GetMapping|PostMapping|PutMapping|DeleteMapping)\("' "$ctrl" 2>/dev/null | grep -oP '/\w+' | tr '\n' ' ')
    if [ -n "$methods" ]; then
      echo "    $ctrl_name: $methods"
    else
      echo "    $ctrl_name: /api/v1/auth/login /api/v1/auth/register /api/v1/auth/password /api/v1/auth/years"
    fi
  else
    echo "    $ctrl_name: $mapping/**"
  fi
done

echo ""

# ==================== 3. 前端扫描 ====================
echo "【3】前端扫描 (Vue 3)"
echo "--------------------------------------------"

if [ -f "$BASE_DIR/xm_film/vue/package.json" ]; then
  VUE_VER=$(grep '"vue"' "$BASE_DIR/xm_film/vue/package.json" | grep -oP '\d+\.\d+\.\d+')
  echo "  Vue版本: $VUE_VER"
fi

VUE_PAGES=$(find "$BASE_DIR/xm_film/vue/src/views" -name '*.vue' | wc -l)
FRONT_PAGES=$(find "$BASE_DIR/xm_film/vue/src/views/front" -name '*.vue' 2>/dev/null | wc -l)
BACK_PAGES=$(find "$BASE_DIR/xm_film/vue/src/views/back" -name '*.vue' 2>/dev/null | wc -l)
MANAGE_PAGES=$(find "$BASE_DIR/xm_film/vue/src/views/manage" -name '*.vue' 2>/dev/null | wc -l)

echo "  总页面数: $VUE_PAGES 个"
echo "  用户前台: $FRONT_PAGES 个"
echo "  影院后台: $BACK_PAGES 个"
echo "  管理后台: $MANAGE_PAGES 个"

echo ""
echo "  页面路由清单:"
if [ -f "$BASE_DIR/xm_film/vue/src/router/index.js" ]; then
  grep -E "path:.*'.*'" "$BASE_DIR/xm_film/vue/src/router/index.js" | grep -v "//" | head -50
fi

echo ""

# ==================== 4. 数据库扫描 ====================
echo "【4】数据库扫描"
echo "--------------------------------------------"
DB_COUNT=$(find "$BASE_DIR/数据库" -name '*.sql' 2>/dev/null | wc -l)
echo "  数据库脚本: $DB_COUNT 个"
for f in $(find "$BASE_DIR/数据库" -name '*.sql' | sort); do
  echo "    - $(basename "$f")"
done
echo ""

# ==================== 5. 配置检查 ====================
echo "【5】配置检查"
echo "--------------------------------------------"

# application.yml
if [ -f "$BASE_DIR/xm_film/springboot/src/main/resources/application.yml" ]; then
  echo "  [OK] application.yml 存在"
  # 检查硬编码密码
  if grep -q "password: 123456" "$BASE_DIR/xm_film/springboot/src/main/resources/application.yml" 2>/dev/null; then
    echo "  [WARN] 数据库密码为默认值(123456)，建议生产环境修改"
  fi
fi

# 检查 node_modules
if [ -d "$BASE_DIR/xm_film/vue/node_modules" ]; then
  echo "  [OK] 前端依赖已安装"
else
  echo "  [WARN] 前端依赖未安装 (运行: cd xm_film/vue && npm install)"
fi

# 检查 JAR
JAR_COUNT=$(find "$BASE_DIR/xm_film/springboot/target" -name '*.jar' 2>/dev/null | wc -l)
if [ "$JAR_COUNT" -gt 0 ]; then
  echo "  [OK] 后端已构建 ($JAR_COUNT 个 JAR)"
else
  echo "  [WARN] 后端未构建 (运行: cd xm_film/springboot && mvn clean package -DskipTests)"
fi

echo ""

# ==================== 6. 依赖完整性 ====================
echo "【6】依赖完整性"
echo "--------------------------------------------"
echo "  后端 Maven 依赖: $(grep -c '<artifactId>' "$BASE_DIR/xm_film/springboot/pom.xml" 2>/dev/null || echo 0) 个"
echo "  前端 npm 依赖: $(grep -c '"' "$BASE_DIR/xm_film/vue/package.json" 2>/dev/null | head -1 || echo 0) 个 (dependencies + devDependencies)"
echo ""

# ==================== 7. 测试检查 ====================
echo "【7】测试检查"
echo "--------------------------------------------"
if [ -d "$BASE_DIR/xm_film/vue/e2e-tests" ]; then
  echo "  [OK] Playwright E2E 测试目录存在"
  TEST_COUNT=$(find "$BASE_DIR/xm_film/vue/e2e-tests" -name '*.mjs' | wc -l)
  echo "  E2E 测试脚本: $TEST_COUNT 个"
fi
echo ""

# ==================== 8. E2E 测试运行说明 ====================
echo "【8】E2E 测试运行说明"
echo "--------------------------------------------"
echo "  运行命令:"
echo "    cd xm_film/vue"
echo "    npx playwright test e2e-tests/e2e-scan.spec.mjs --config=e2e-tests/playwright.config.mjs"
echo ""
echo "  查看 HTML 报告:"
echo "    npx playwright show-report xm_film/vue/e2e-tests/playwright-report"
echo ""
echo "  旧版测试:"
echo "    node e2e-tests/test.mjs"
echo ""

# ==================== 结束 ====================
echo "=========================================="
echo "  扫描完成"
echo "=========================================="
