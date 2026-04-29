/**
 * 格式化日期时间为 YYYY-MM-DD HH:mm:ss
 */
export function formatDateTime(dateStr: string | number | Date | null | undefined): string {
  if (!dateStr) return ''
  
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return String(dateStr)
  
  const pad = (num: number) => String(num).padStart(2, '0')
  
  const y = date.getFullYear()
  const m = pad(date.getMonth() + 1)
  const d = pad(date.getDate())
  const h = pad(date.getHours())
  const min = pad(date.getMinutes())
  const s = pad(date.getSeconds())
  
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

/**
 * 格式化日期为 YYYY-MM-DD
 */
export function formatDate(dateStr: string | number | Date | null | undefined): string {
  if (!dateStr) return ''
  
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return String(dateStr)
  
  const pad = (num: number) => String(num).padStart(2, '0')
  
  const y = date.getFullYear()
  const m = pad(date.getMonth() + 1)
  const d = pad(date.getDate())
  
  return `${y}-${m}-${d}`
}
