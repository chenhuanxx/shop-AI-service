/**
 * 环境配置 - 统一管理不同环境的访问地址
 */

// API 基础地址（根据环境自动读取）
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// API 文档地址
export const API_DOCS_URL = import.meta.env.VITE_API_DOCS_URL || '/docs'

/**
 * 获取完整的文件上传地址
 */
export function getUploadUrl(): string {
  return `${API_BASE_URL}/upload`
}

/**
 * 获取完整的文件访问地址
 * @param path 文件路径，如 /uploads/xxx.png
 */
export function getFileUrl(path: string): string {
  if (!path) return ''
  // 如果已经是完整 URL，直接返回
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  // 如果是 /uploads/xxx 格式，拼接 API 基础地址
  if (path.startsWith('/')) {
    return `${API_BASE_URL}${path}`
  }
  // 相对路径
  return `${API_BASE_URL}/${path}`
}

/**
 * 判断当前是否为生产环境
 */
export const isProduction = import.meta.env.VITE_APP_ENV === 'production'
