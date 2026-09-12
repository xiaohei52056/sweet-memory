<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import StarfieldCanvas from './components/StarfieldCanvas.vue'
import Lightbox from './components/Lightbox.vue'

const route = useRoute()
// 后台不需要星空背景
const showStarfield = computed(() => !route.path.startsWith('/admin'))
</script>

<template>
  <StarfieldCanvas v-if="showStarfield" />
  <router-view />
  <Lightbox />
</template>

<style scoped>
/* StarfieldCanvas 自身 fixed 铺底 */
</style>
