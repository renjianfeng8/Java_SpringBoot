package com.example.springboot;

import com.example.springboot.common.FileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 导入路径与上传路径必须产出同一种文件名形态，否则媒体目录里会同时存在
 * 「上传的」和「导入的」两套命名，后续任何按名归类的逻辑都会分叉。
 */
class FileUtilTest {

    @Test
    void saveBytesUsesTheSameNamingRuleAsUpload(@TempDir Path dir) throws IOException {
        byte[] content = {1, 2, 3};

        String fileName = FileUtil.saveBytes(content, ".jpg", dir.toString());

        // UUID 是 36 字符，加 ".jpg" 共 40
        assertThat(fileName).endsWith(".jpg").hasSize(40);
        assertThat(Files.readAllBytes(dir.resolve(fileName))).containsExactly((byte) 1, (byte) 2, (byte) 3);
    }

    @Test
    void saveBytesCreatesMissingDirectory(@TempDir Path dir) throws IOException {
        Path nested = dir.resolve("not-yet").resolve("deeper");

        String fileName = FileUtil.saveBytes(new byte[]{9}, ".png", nested.toString());

        assertThat(nested.resolve(fileName)).exists();
    }

    /** 同一张海报导入两次不得互相覆盖 */
    @Test
    void namesAreUniqueSoRepeatedImportsDoNotCollide(@TempDir Path dir) throws IOException {
        String first = FileUtil.saveBytes(new byte[]{1}, ".jpg", dir.toString());
        String second = FileUtil.saveBytes(new byte[]{1}, ".jpg", dir.toString());

        assertThat(first).isNotEqualTo(second);
    }

    /** 导入时扩展名取自 TMDB 的图片路径（"/qkr....jpg"），不是完整 URL */
    @Test
    void extensionComesFromTmdbImagePath() {
        assertThat(FileUtil.getFileExtension("/qkrH7rjYKU4KOwwIl6wF6H3ISvm.jpg")).isEqualTo(".jpg");
        assertThat(FileUtil.getFileExtension("/no-extension")).isEmpty();
        assertThat(FileUtil.getFileExtension(null)).isEmpty();
    }
}
