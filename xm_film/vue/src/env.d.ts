/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly DEV: boolean; // 是否为开发环境
    readonly PROD: boolean; // 是否为生产环境
    readonly VITE_BASE_API: string; // 你的 API 基础路径
    // 可添加其他环境变量（按项目实际需求）
    // readonly VITE_ANOTHER_VAR: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}