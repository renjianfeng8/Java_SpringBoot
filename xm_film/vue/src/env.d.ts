/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly DEV: boolean; // 是否为开发环境
    readonly PROD: boolean; // 是否为生产环境
    // API 基础路径。可能为 undefined —— `.env` 不入库（见 Bug.md BUG-023），
    // 全新克隆只有 `.env.development`，生产构建则由部署环境决定；消费处必须给回退值（constants/index.js 兜底为 `/`）
    readonly VITE_API_BASE_URL: string | undefined;
    // 可添加其他环境变量（按项目实际需求）
    // readonly VITE_ANOTHER_VAR: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}