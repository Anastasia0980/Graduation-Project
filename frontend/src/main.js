import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import Root from './Root.vue'
import router from './router'
import { installGlobalAuthInterceptor } from './utils/http'

installGlobalAuthInterceptor(router)

createApp(Root)
  .use(router)
  .use(ElementPlus)
  .mount('#app')
