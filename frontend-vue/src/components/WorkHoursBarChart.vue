<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'

type Item = {
  name: string
  value: number
}

type Props = {
  title?: string
  data?: Item[]
  editable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '工时统计',
  data: () => [
    { name: '国发银行', value: 19 },
    { name: '档案系统', value: 17 },
    { name: '审计系统', value: 16 },
    { name: '人力资源', value: 16 },
    { name: '股权系统', value: 14 },
  ],
  editable: true,
})

const items = ref<Item[]>(props.data.map((x) => ({ name: x.name, value: x.value })))
const barAreaEls = ref<(HTMLDivElement | null)[]>([])
const isDragging = ref(false)
const axisMaxFrozen = ref<number | null>(null)

const maxValue = computed(() => {
  if (isDragging.value && axisMaxFrozen.value !== null) return axisMaxFrozen.value
  const m = Math.max(1, ...items.value.map((x) => x.value || 0))
  const ceilTo = 2
  const base = Math.ceil(m / ceilTo) * ceilTo
  const needHeadroom = items.value.some((x) => Math.round(x.value) === base)
  return needHeadroom ? base + 5 : base
})

const ticks = computed(() => {
  const max = maxValue.value
  const step = max <= 10 ? 1 : 2
  const out: number[] = []
  for (let v = 0; v <= max; v += step) out.push(v)
  return out
})

const draggingIndex = ref<number | null>(null)
const dragStartX = ref(0)
const dragStartValue = ref(0)
const dragWidth = ref(1)

function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n))
}

function onDragStart(index: number, e: PointerEvent) {
  if (!props.editable) return
  const barArea = barAreaEls.value[index]
  if (!barArea) return

  const m = Math.max(1, ...items.value.map((x) => x.value || 0))
  const ceilTo = 2
  const base = Math.ceil(m / ceilTo) * ceilTo
  const needHeadroom = items.value.some((x) => Math.round(x.value) === base)
  axisMaxFrozen.value = needHeadroom ? base + 5 : base
  isDragging.value = true

  const rect = barArea.getBoundingClientRect()
  draggingIndex.value = index
  dragStartX.value = e.clientX
  dragStartValue.value = items.value[index]?.value ?? 0
  dragWidth.value = Math.max(1, rect.width - 28)

  window.addEventListener('pointermove', onDragMove)
  window.addEventListener('pointerup', onDragEnd)
  window.addEventListener('pointercancel', onDragEnd)
}

function onDragMove(e: PointerEvent) {
  const idx = draggingIndex.value
  if (idx === null) return
  const dx = e.clientX - dragStartX.value
  const deltaValue = (dx / dragWidth.value) * maxValue.value
  const next = clamp(dragStartValue.value + deltaValue, 0, maxValue.value)
  items.value[idx].value = Math.round(next)
}

function onDragEnd() {
  draggingIndex.value = null
  window.removeEventListener('pointermove', onDragMove)
  window.removeEventListener('pointerup', onDragEnd)
  window.removeEventListener('pointercancel', onDragEnd)
  isDragging.value = false
  axisMaxFrozen.value = null
}

onUnmounted(() => {
  onDragEnd()
})

const addOpen = ref(false)
const addName = ref('')
const addValue = ref<number | ''>('')

function doAdd() {
  const name = String(addName.value || '').trim()
  const raw = addValue.value
  const val = typeof raw === 'number' ? raw : Number(raw)
  if (!name) return
  if (!Number.isFinite(val) || val < 0) return
  items.value.push({ name, value: Math.round(val) })
  addName.value = ''
  addValue.value = ''
  addOpen.value = false
}
</script>

<template>
  <section class="wh-card">
    <div class="wh-head">
      <div class="wh-title">{{ props.title }}</div>
      <div class="wh-actions">
        <button class="wh-btn" @click="addOpen = !addOpen">{{ addOpen ? '取消' : '添加纵坐标' }}</button>
      </div>
    </div>

    <div class="wh-chart">
      <div class="wh-grid">
        <div v-for="t in ticks" :key="t" class="wh-grid-col" :style="{ left: `${(t / maxValue) * 100}%` }"></div>
      </div>

      <div v-if="addOpen" class="wh-add">
        <input class="wh-input" v-model="addName" placeholder="名称" />
        <input class="wh-input" v-model="addValue" placeholder="工时" type="number" min="0" />
        <button class="wh-btn wh-btn-primary" @click="doAdd">添加</button>
      </div>

      <div class="wh-rows">
        <div v-for="(it, i) in items" :key="it.name" class="wh-row">
          <div class="wh-label">{{ it.name }}</div>
          <div :ref="(el) => (barAreaEls[i] = el as HTMLDivElement)" class="wh-bar-area">
            <div class="wh-bar" :style="{ width: `${(it.value / maxValue) * 100}%` }">
              <div
                class="wh-handle"
                :class="{ dragging: draggingIndex === i, disabled: !props.editable }"
                @pointerdown="onDragStart(i, $event)"
              >
                <span class="wh-grip"></span>
              </div>
            </div>
            <div
              class="wh-value"
              :style="{ left: `calc(${(it.value / maxValue) * 100}% + 10px)` }"
            >
              {{ it.value }}
            </div>
          </div>
        </div>
      </div>

      <div class="wh-axis">
        <div v-for="t in ticks" :key="t" class="wh-tick" :style="{ left: `${(t / maxValue) * 100}%` }">
          {{ t }}
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="less">
.wh-card {
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(14px);
  padding: 18px 18px 14px ;
  margin-top: 14px;
}

.wh-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.wh-title {
  font-weight: 700;
  color: #e5e7eb;
}

.wh-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wh-btn {
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  color: #e5e7eb;
  padding: 6px 10px;
}

.wh-btn-primary {
  border-color: rgba(34, 197, 94, 0.35);
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.35), rgba(16, 185, 129, 0.22));
}

.wh-chart {
  position: relative;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 14px;
  padding: 14px 14px 34px;
  color: #111827;
  overflow: hidden;
}

.wh-add {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.wh-input {
  height: 28px;
  border-radius: 8px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  padding: 0 8px;
}

.wh-grid {
  position: absolute;
  left: 124px;
  right: 14px;
  top: 14px;
  bottom: 34px;
  pointer-events: none;
}

.wh-grid-col {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: rgba(17, 24, 39, 0.08);
}

.wh-rows {
  position: relative;
  display: grid;
  gap: 12px;
}

.wh-row {
  display: grid;
  grid-template-columns: 110px 1fr;
  align-items: center; 
}

.wh-label {
  font-size: 12px;
  color: rgba(17, 24, 39, 0.72);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wh-bar-area {
  position: relative;
  height: 18px;
}

.wh-bar {
  position: absolute;
  left: 0;
  top: 0;
  height: 18px;
  border-radius: 0;
  background: #10b981;
}

.wh-value {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  left: 0;
  font-size: 12px;
  color: rgba(17, 24, 39, 0.78);
  min-width: 24px;
  pointer-events: none;
}

.wh-handle {
  position: absolute;
  top: 50%;
  right: -10px;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border-radius: 999px;
  border: 1px solid rgba(17, 24, 39, 0.18);
  background: #ffffff;
  display: grid;
  place-items: center;
  cursor: ew-resize;
  box-shadow: 0 10px 18px rgba(0, 0, 0, 0.08);
  user-select: none;
}

.wh-handle.disabled {
  cursor: default;
  opacity: 0.55;
}

.wh-handle.dragging {
  border-color: rgba(16, 185, 129, 0.8);
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.18);
}

.wh-grip {
  width: 10px;
  height: 10px;
  background:
    radial-gradient(circle, rgba(17, 24, 39, 0.55) 2px, transparent 3px) 0 0 / 6px 6px,
    radial-gradient(circle, rgba(17, 24, 39, 0.55) 2px, transparent 3px) 3px 3px / 6px 6px;
}

.wh-axis {
  position: absolute;
  left: 124px;
  right: 14px;
  bottom: 10px;
  height: 18px;
}

.wh-tick {
  position: absolute;
  transform: translateX(-50%);
  bottom: 0;
  font-size: 11px;
  color: rgba(17, 24, 39, 0.58);
}

@media (max-width: 720px) {
  .wh-row {
    grid-template-columns: 90px 1fr;
  }
  .wh-axis {
    left: 104px;
  }
  .wh-grid {
    left: 104px;
    right: 14px;
    top: 14px;
    bottom: 34px;
  }
}
</style>
