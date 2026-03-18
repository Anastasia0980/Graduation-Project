import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import TaskDetail from '../views/TaskDetail.vue'
import StudentProfile from '../views/StudentProfile.vue'
import StudentHistory from '../views/StudentHistory.vue'
import Ranking from '../views/Ranking.vue'

import TeacherHome from '../views/TeacherHome.vue'
import TeacherPublishTask from '../views/TeacherPublishTask.vue'
import TeacherTaskManage from '../views/TeacherTaskManage.vue'
import TeacherClassData from '../views/TeacherClassData.vue'
import TeacherExportScore from '../views/TeacherExportScore.vue'
import TeacherTaskHall from '../views/TeacherTaskHall.vue'

import StudentClassJoin from '../views/StudentClassJoin.vue'
import StudentTournament from '../views/StudentTournament.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: Home
  },
  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/register',
    name: 'register',
    component: Register
  },
  {
    path: '/battle',
    redirect: '/task-detail'
  },
  {
    path: '/task-detail',
    name: 'taskDetail',
    component: TaskDetail
  },
  {
    path: '/student/profile',
    name: 'studentProfile',
    component: StudentProfile
  },
  {
    path: '/student/history',
    name: 'studentHistory',
    component: StudentHistory
  },
  {
    path: '/student/ranking',
    name: 'studentRanking',
    component: Ranking
  },
  {
    path: '/student/class',
    name: 'studentClass',
    component: StudentClassJoin
  },
  {
    path: '/student/tournament',
    name: 'studentTournament',
    component: StudentTournament
  },

  {
    path: '/teacher/home',
    name: 'teacherHome',
    component: TeacherHome
  },
  {
    path: '/teacher/hall',
    name: 'teacherHall',
    component: TeacherTaskHall
  },
  {
    path: '/teacher/publish',
    name: 'teacherPublish',
    component: TeacherPublishTask
  },
  {
    path: '/teacher/tasks',
    name: 'teacherTasks',
    component: TeacherTaskManage
  },
  {
    path: '/teacher/classes',
    name: 'teacherClasses',
    component: TeacherClassData
  },
  {
    path: '/teacher/export',
    name: 'teacherExport',
    component: TeacherExportScore
  },
  {
    path: '/teacher/student-detail/:studentId',
    name: 'TeacherStudentDetail',
    component: () => import('../views/TeacherStudentDetail.vue')
  },
  {
    path: '/teacher/task-edit/:taskId',
    name: 'TeacherTaskEdit',
    component: () => import('../views/TeacherTaskEdit.vue')
  },
  {
    path: '/teacher/task-submissions/:taskId',
    name: 'TeacherTaskSubmissions',
    component: () => import('../views/TeacherTaskSubmissions.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
