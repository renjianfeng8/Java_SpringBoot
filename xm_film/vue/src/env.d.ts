/// <reference types="vite/client" />

interface ImportMetaEnv {
    // API 基础路径。可能为 undefined —— `.env` 不入库，
    // 全新克隆只有 `.env.development`；消费处必须给回退值（constants/index.js 兜底为 `/`）
    readonly VITE_API_BASE_URL: string | undefined;
    // 可添加其他环境变量（按项目实际需求）
    // readonly VITE_ANOTHER_VAR: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}