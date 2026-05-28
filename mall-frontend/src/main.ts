import 'element-plus/dist/index.css';
import '@/styles/base.css';

import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { createApp } from 'vue';

import App from '@/App.vue';
import router from '@/router';

createApp(App).use(createPinia()).use(ElementPlus).use(router).mount('#app');
