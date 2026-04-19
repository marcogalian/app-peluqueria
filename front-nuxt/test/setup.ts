import * as Vue from 'vue'
import { defineStore, setActivePinia, createPinia } from 'pinia'

Object.assign(globalThis, {
  ref: Vue.ref,
  computed: Vue.computed,
  reactive: Vue.reactive,
  watch: Vue.watch,
  watchEffect: Vue.watchEffect,
  toRef: Vue.toRef,
  toRefs: Vue.toRefs,
  isRef: Vue.isRef,
  unref: Vue.unref,
  nextTick: Vue.nextTick,
  defineStore,
})
