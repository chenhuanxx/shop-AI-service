<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import * as echarts from "echarts";

import HomeHeader from "../../components/HomeHeader.vue";
import { useAuthStore } from "../../stores/auth";

const authStore = useAuthStore();
const router = useRouter();
const colorList = [
  "linear-gradient(135deg, #2B7FFF 0%, #155DFC 100%)",
  "linear-gradient(135deg, #AD46FF 0%, #9810FA 100%)",
  "linear-gradient(135deg, #00C950 0%, #00A63E 100%)",
  "linear-gradient(135deg, #F6339A 0%, #E60076 100%)",
  "linear-gradient(135deg, #FF6900 0%, #F54900 100%)",
  "linear-gradient(135deg, #00BBA7 0%, #009689 100%)",
  "linear-gradient(135deg, #615FFF 0%, #4F39F6 100%)",
  "linear-gradient(135deg, #FF2056 0%, #EC003F 100%)",
];

const weekTotal = 27;
const weekDone = 11;
const weekPlanned = 22;
const weekTemp = 5;

const pieRef = ref<HTMLDivElement | null>(null);
const trendRef = ref<HTMLDivElement | null>(null);
const projectRef = ref<HTMLDivElement | null>(null);
const planRef = ref<HTMLDivElement | null>(null);

let pieChart: echarts.ECharts | null = null;
let trendChart: echarts.ECharts | null = null;
let projectChart: echarts.ECharts | null = null;
let planChart: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;
let trendShowTipHandler: ((params: unknown) => void) | null = null;
let trendHideTipHandler: ((params: unknown) => void) | null = null;

function initChart(el: HTMLDivElement, option: echarts.EChartsOption) {
  const chart = echarts.init(el, undefined, { renderer: "canvas" });
  chart.setOption(option, { notMerge: true });
  return chart;
}

async function onLogout() {
  authStore.logout();
  await router.replace("/login");
}

function onRefresh() {
  pieChart?.resize();
  trendChart?.resize();
  projectChart?.resize();
  planChart?.resize();
}

onMounted(() => {
  const pieEl = pieRef.value;
  const trendEl = trendRef.value;
  const projectEl = projectRef.value;
  const planEl = planRef.value;

  if (pieEl) {
    const done = weekDone;
    const undone = Math.max(weekTotal - weekDone - weekTemp, 0);
    const pieDoneColor = "#3B82F6";
    const pieUndoneColor = "#F59E0B";
    const pieTempColor = "#009966";
    pieChart = initChart(pieEl, {
      tooltip: { trigger: "item" },
      legend: { show: false },
      series: [
        {
          type: "pie",
          avoidLabelOverlap: true,
          label: {
            formatter: (p: { name: string; percent?: number }) => {
              const pct =
                typeof p.percent === "number" ? `${p.percent.toFixed(2)}%` : "";
              return `{n|${p.name}}  {p|${pct}}`;
            },
            rich: {
              n: { fontSize: 12, color: "#111827" },
              p: { fontSize: 12, color: "#2563eb", fontWeight: 600 },
            },
          },
          labelLine: { length: 12, length2: 10 },
          itemStyle: { borderColor: "#ffffff", borderWidth: 2 },
          data: [
            {
              name: "完成",
              value: done,
              itemStyle: { color: pieDoneColor },
              label: {
                borderColor: pieDoneColor,
                borderWidth: 1,
                borderRadius: 4,
                padding: [6, 10],
                rich: { p: { color: pieDoneColor, fontWeight: 600 } },
              },
            },
            {
              name: "未完成",
              value: undone,
              itemStyle: { color: pieUndoneColor },
              label: {
                borderColor: pieUndoneColor,
                borderWidth: 1,
                borderRadius: 4,
                padding: [6, 10],
                rich: { p: { color: pieUndoneColor, fontWeight: 600 } },
              },
            },
            {
              name: "临时",
              value: weekTemp,
              itemStyle: { color: pieTempColor },
              label: {
                borderColor: pieTempColor,
                borderWidth: 1,
                borderRadius: 4,
                padding: [6, 10],
                rich: { p: { color: pieTempColor, fontWeight: 600 } },
              },
            },
          ],
        },
      ],
    });
  }

  if (trendEl) {
    trendChart = initChart(trendEl, {
      grid: { left: 42, right: 18, top: 30, bottom: 34 },
      tooltip: { trigger: "axis" },
      xAxis: {
        type: "category",
        data: [
          "6/30-7/6",
          "7/7-7/13",
          "7/14-7/20",
          "7/21-7/27",
          "7/28-8/3",
          "8/4-8/10",
        ],
        axisTick: { show: false },
        axisLine: { lineStyle: { color: "#e5e7eb" } },
        axisLabel: { color: "#6b7280", fontSize: 12 },
      },
      yAxis: {
        type: "value",
        axisLine: { show: false },
        splitLine: { lineStyle: { color: "#eef2ff" } },
        axisLabel: { color: "#6b7280", fontSize: 12 },
      },
      series: [
        {
          id: "trend-bar",
          name: "rate",
          type: "bar",
          data: [72, 46, 58, 80, 74, 90],
          barWidth: 18,
          itemStyle: { borderRadius: [10, 10, 0, 0], color: "#3b82f6" },
          showBackground: false,
          backgroundStyle: {
            color: "rgba(59, 130, 246, 0.10)",
            borderRadius: [10, 10, 0, 0],
          },
        },
      ],
    });

    trendShowTipHandler = (params: unknown) => {
      const p = params as { seriesId?: string };
      if (p.seriesId && p.seriesId !== "trend-bar") return;
      trendChart?.setOption({
        series: [{ id: "trend-bar", showBackground: true }],
      });
    };
    trendHideTipHandler = () => {
      trendChart?.setOption({
        series: [{ id: "trend-bar", showBackground: false }],
      });
    };
    trendChart.on("showTip", trendShowTipHandler);
    trendChart.on("hideTip", trendHideTipHandler);
  }

  if (projectEl) {
    projectChart = initChart(projectEl, {
      grid: { left: 46, right: 18, top: 20, bottom: 74 },
      tooltip: { trigger: "axis" },
      legend: { bottom: 10 },
      xAxis: {
        type: "category",
        data: [
          "国发银行",
          "档案系统",
          "审计系统",
          "人力资源",
          "会员系统",
          "股权系统",
          "商报系统",
          "会议室",
          "其他",
        ],
        axisTick: { show: false },
        axisLine: { lineStyle: { color: "#e5e7eb" } },
        axisLabel: { color: "#6b7280", fontSize: 12 },
      },
      yAxis: {
        type: "value",
        axisLine: { show: false },
        splitLine: { lineStyle: { color: "#eef2ff" } },
        axisLabel: { color: "#6b7280", fontSize: 12 },
      },
      series: [
        {
          name: "完成",
          type: "bar",
          barWidth: 16,
          itemStyle: { color: "#3b82f6", borderRadius: [8, 8, 0, 0] },
          data: [13, 2, 3, 2, 3, 2, 2, 1, 1],
        },
        {
          name: "进行中",
          type: "bar",
          barWidth: 16,
          itemStyle: { color: "#10b981", borderRadius: [8, 8, 0, 0] },
          data: [1, 0, 1, 1, 0, 0, 0, 0, 1],
        },
        {
          name: "延期",
          type: "bar",
          barWidth: 16,
          itemStyle: { color: "#f59e0b", borderRadius: [8, 8, 0, 0] },
          data: [1, 1, 0, 0, 0, 0, 0, 0, 0],
        },
      ],
    });
  }

  if (planEl) {
    planChart = initChart(planEl, {
      grid: { left: 46, right: 18, top: 30, bottom: 34 },
      tooltip: { trigger: "axis" },
      xAxis: {
        type: "category",
        data: [
          "国发银行",
          "档案系统",
          "审计系统",
          "人力资源",
          "会员系统",
          "股权系统",
          "商报系统",
          "会议室",
          "其他",
        ],
        axisTick: { show: false },
        axisLine: { lineStyle: { color: "#e5e7eb" } },
        axisLabel: { color: "#6b7280", fontSize: 12 },
      },
      yAxis: {
        type: "value",
        axisLine: { show: false },
        splitLine: { lineStyle: { color: "#eef2ff" } },
        axisLabel: { color: "#6b7280", fontSize: 12 },
      },
      series: [
        {
          name: "任务数量",
          type: "bar",
          data: [10, 3, 1, 2, 2, 3, 4, 1, 2],
          barWidth: 22,
          itemStyle: {
            borderRadius: [10, 10, 0, 0],
            color: (p: { dataIndex: number }) => {
              const colors = [
                "#3b82f6",
                "#06b6d4",
                "#a78bfa",
                "#22c55e",
                "#6366f1",
                "#3b82f6",
                "#06b6d4",
                "#a78bfa",
                "#64748b",
              ];
              return colors[p.dataIndex] || "#3b82f6";
            },
          },
        },
      ],
    });
  }

  resizeObserver = new ResizeObserver(() => {
    pieChart?.resize();
    trendChart?.resize();
    projectChart?.resize();
    planChart?.resize();
  });

  if (pieEl) resizeObserver.observe(pieEl);
  if (trendEl) resizeObserver.observe(trendEl);
  if (projectEl) resizeObserver.observe(projectEl);
  if (planEl) resizeObserver.observe(planEl);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  resizeObserver = null;

  pieChart?.dispose();
  if (trendChart && trendShowTipHandler)
    trendChart.off("showTip", trendShowTipHandler);
  if (trendChart && trendHideTipHandler)
    trendChart.off("hideTip", trendHideTipHandler);
  trendChart?.dispose();
  projectChart?.dispose();
  planChart?.dispose();

  pieChart = null;
  trendChart = null;
  projectChart = null;
  planChart = null;
  trendShowTipHandler = null;
  trendHideTipHandler = null;
});

const rankList = [
  { key: "TOP1", userList: ["L", "LM", "A", "JTS"], value: 9 },
  { key: "TOP2", userList: ["LM", "A"], value: 8 },
  { key: "TOP3", userList: ["A"], value: 6 },
  { key: "TOP4", userList: ["JTS", "A", "JTS", "E"], value: 6  },
  { key: "TOP5", userList: ["E"], value: 3 },
];
</script>

<template> 
     <div class="pa-list-page">
    <div class="pa-list-bg">
      <div class="pa-list-orb pa-list-orb-a"></div>
      <div class="pa-list-orb pa-list-orb-b"></div>
      <div class="pa-list-orb pa-list-orb-c"></div>
      <div class="pa-list-grid"></div>
    </div>
    <HomeHeader title="python-admin" subtitle="仪表盘 · ECharts" @logout="onLogout" @refresh="onRefresh" />

    <main class="dash-main">
      <section class="grid">
        <div class="panel panel-lg">
          <div class="panel-head">
            <div>
              <div class="panel-title">本周事项总览</div>
              <div class="panel-sub">计划事项与临时事项的完成情况</div>
            </div>
          </div>

          <div class="week-body">
            <div class="week-left">
              <div class="week-big">
                <div class="week-done">{{ weekDone }}</div>
                <div class="week-total">/ {{ weekTotal }} 项</div>
              </div>

              <div class="mini-grid">
                <div class="mini-card mini-blue">
                  <div class="mini-ico">
                    <img src="../../assets/images/ybp/jhsx.svg" alt="" />
                  </div>
                  <div class="mini-meta">
                    <div class="mini-name">计划事项</div>
                    <div class="mini-val">
                      {{ weekPlanned }} <span class="mini-unit">项</span>
                    </div>
                  </div>
                </div>

                <div class="mini-card mini-green">
                  <div class="mini-ico">
                    <img src="../../assets/images/ybp/lssx.svg" alt="" />
                  </div>
                  <div class="mini-meta">
                    <div class="mini-name">临时事项</div>
                    <div class="mini-val">
                      {{ weekTemp }} <span class="mini-unit">项</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="week-right">
              <div ref="pieRef" class="chart chart-pie"></div>
            </div>
          </div>
        </div>

        <div class="panel ">
          <div class="panel-head">
            <div>
              <div class="panel-title">效率趋势</div>
              <div class="panel-sub">最近 6 周完成效率统计</div>
            </div>
          </div>
          <div ref="trendRef" class="chart chart-bar"></div>
        </div>

        <div class="panel panel-lg">
          <div class="panel-head">
            <div>
              <div class="panel-title">本周各项目完成情况</div>
              <div class="panel-sub">本周任务分布与完成统计</div>
            </div>
          </div>
          <div ref="projectRef" class="chart chart-bar"></div>
        </div>

        <div class="panel">
          <div class="panel-head">
            <div>
              <div class="panel-title">任务数据排行榜</div>
              <div class="panel-sub">任务数量 TOP5</div>
            </div>
          </div>

          <div class="rank">
            <div v-for="(item, index) in rankList" :key="index" class="rank-row" :class="{'rank-row-active': index%2 === 0}">
              <div class="rank-badge">
                <img src="../../assets/images/ybp/top1.svg" v-if="index === 0" alt="" />
                <img src="../../assets/images/ybp/top2.svg" v-else-if="index === 1" alt="" />
                <img src="../../assets/images/ybp/top3.svg" v-else-if="index === 2" alt="" />
                <img src="../../assets/images/ybp/top4.svg" v-else-if="index === 3" alt="" />
                <img src="../../assets/images/ybp/top5.svg" v-else-if="index === 4" alt="" />
              </div>
              <div class="rank-avatar-container">
                <div class="rank-avatar" v-for="(user, i) in item.userList" :key="i">
                  <div
                    class="avatar"
                    :class="'bg' + i"
                    :style="{ background: colorList[(index * 3 + i) % colorList.length] }"
                    v-if="i < 3"
                  >
                    {{ user[0] }}
                  </div>
                  <div class="avatar bg4" v-if="i == 3">+</div>
                </div>
              </div>

              <div class="rank-val">{{ item.value }}</div>
              <div class="rank-bar-box">
                <div class="rank-bar">
                  <div class="rank-bar-fill" :style="{ width: `${(item.value / 10) * 100}%` }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
 
      <section class="grid2">
        <div class="panel-full">
          <div class="panel-head">
            <div>
              <div class="panel-title">下周计划分布</div>
              <div class="panel-sub">下周各项目任务规划概览</div>
            </div>
          </div>
          <div ref="planRef" class="chart chart-bar chart-plan"></div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped lang="less">
.pa-list-page { 
  
  .dash-main {
    height: 100vh;
    overflow: auto;
    margin-left: 240px;
    padding: 22px 22px 36px;
    box-sizing: border-box;
  }

  .grid {
    width: 1264px;
    margin: 0 auto;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 18px;
    overflow: hidden;
  }
  
  .grid2 {
    width: 1264px;
    margin: 0 auto;
    display: grid;  
  }
}


.panel {
  width: 466px;
  border-radius: 14px;
  border: 1px solid #e8e8eb;
  background: linear-gradient(180deg, #f0f5ff 6.75%, #fff 18.88%);
  box-shadow: 0 3px 20px 0 rgba(0, 0, 0, 0.04);
  padding: 30px 30px;
  height: 400px;
  overflow: hidden;
}

.panel-lg {
  width: 780px;
  height: 400px;
}

.panel-full { 
  width: 100%;
  border-radius: 14px;
  border: 1px solid #e8e8eb;
  background: linear-gradient(180deg, #f0f5ff 6.75%, #fff 18.88%);
  box-shadow: 0 3px 20px 0 rgba(0, 0, 0, 0.04);
  padding: 30px 30px;
  margin-top: 18px;
  height: 400px;
  overflow: hidden; 
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.panel-title {
  color: #000;
  font-family: "PingFang SC";
  font-size: 18px;
  font-style: normal;
  font-weight: 600;
  line-height: 28px;
  /* 155.556% */
}

.panel-sub {
  color: #99a1af;
  font-family: "PingFang SC";
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
  line-height: 16px;
  /* 133.333% */
}

.week-body {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr; 
  align-items: center;
  flex: 1;
}

.week-left {
  display: flex;
  flex-direction: column;
  width: 280px;
}

.week-big {
  display: flex;
  align-items: baseline;
  padding: 6px 2px 0;
}

.week-done {
  color: #101828;
  font-family: Avenir;
  font-size: 48px;
  font-style: normal;
  font-weight: 800;
  line-height: 48px;
  /* 100% */
  letter-spacing: 0.352px;
}

.week-total {
  color: #6a7282;
  font-family: Avenir;
  font-size: 24px;
  font-style: normal;
  font-weight: 500;
  line-height: 32px;
  /* 133.333% */
  letter-spacing: 0.07px;
}

.mini-grid {
  margin-top: 32px;
  width: 100%;
}

.mini-card {
  width: 100%;
  height: 90px;
  border-radius: 14px;
  font-family: "PingFang SC";
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  line-height: 16px;
  /* 133.333% */
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.mini-blue {
  border: 1px solid #dbeafe;
  background: linear-gradient(90deg, #eff6ff 0%, rgba(239, 246, 255, 0.5) 100%);
  color: #096;
}

.mini-green {
  border-radius: 14px;
  border: 1px solid #d0fae5;
  background: linear-gradient(90deg, #ecfdf5 0%, rgba(236, 253, 245, 0.5) 100%);
}

.mini-ico {
  display: flex;
  padding: 12px;
  justify-content: center;
  align-items: center;

  img {
    width: 48px;
    height: 48px;
  }
}

.mini-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mini-name {
  font-size: 12px;
  opacity: 0.9;
}

.mini-val {
  font-size: 22px;
  font-weight: 800;
  color: #111827;
}

.mini-unit {
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  margin-left: 2px;
}

.week-right {
  width: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

.chart {
  width: 100%;
  height: 290px;
}

.chart-pie {
  height: 180px;
}

.chart-plan {
  height: 260px;
}

.rank {
  display: flex;
  flex-direction: column; 
  padding-top: 8px;

  .rank-row {
    border-radius: 12px;
    height: 56px;
    display: flex;
    align-items: center;
    width: 100%;
    overflow: hidden;
    &.rank-row-active {
    background: #f9fafb;
    }

    .rank-badge {
      margin-left: 20px;
      display: flex;
      align-items: center;
      height: 56px;

      img {
        width: 48px;
        height: 40px;
      }
    }

    .rank-avatar-container { 
      flex: 1;
      display: flex;
      align-items: center;
      height: 56px;
      position: relative;
      margin-left: 12px;

      .rank-avatar {
        width: 30px;
        height: 30px;
      }

      .avatar {
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 33554400px;
        border: 2px solid #fff;
        box-shadow:
          0 1px 3px 0 rgba(0, 0, 0, 0.1),
          0 1px 2px -1px rgba(0, 0, 0, 0.1);

        color: #fff;
        font-family: Inter;
        font-size: 14px;
        font-style: normal;
        font-weight: 400;
        letter-spacing: -0.15px;
        position: absolute;
      }
    }
  }
}

.bg0 {
  background: linear-gradient(135deg, #2b7fff 0%, #155dfc 100%);
  left: 0;
  z-index: 9;
}

.bg1 {
  background: linear-gradient(135deg, #2b7fff 0%, #155dfc 100%);
  left: 25px;
  z-index: 8;
}

.bg2 {
  background: linear-gradient(135deg, #2b7fff 0%, #155dfc 100%);
  left: 50px;
  z-index: 7;
}

.bg4 {
  background: linear-gradient(135deg, #858687 0%, #3d3e40 100%);
  left: 75px;
  z-index: 5;
}

.rank-val {
  color: #6a7282;
  text-align: right;
  font-family: Inter;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
  line-height: 16px;
  /* 133.333% */
  margin-right: 20px;
}

.rank-bar-box {
  width: 116px;
  height: 8px;
  margin-right: 20px;
  display: flex;
  align-items: center;
}

.rank-bar {
  width: 100%;
  align-self: stretch;
  height: 8px;
  background: #f3f4f6;
  border-radius: 33554400px;
  overflow: hidden;
}

.rank-bar-fill {
  height: 100%;
  border-radius: 33554400px;
  background: linear-gradient(90deg, #00d5be 0%, #00bc7d 100%);
}

@media (max-width: 1140px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .panel-full {
    grid-column: auto;
  }

  .rank-row {
    grid-template-columns: 140px 1fr 30px;
  }
}
</style>
