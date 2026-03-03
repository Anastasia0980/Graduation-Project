# RL_OJ

## 运行环境

- 前端：node.js 18.20.8

  ​	   vue3

- 后端：java 17

  ​	   springboot 3.5.11

- python：python 3.9

  ​	      requirements.txt为虚拟环境包

## 文件目录

- frontend文件夹：前端文件，基本就是初始化了一个前端文件夹，改动地方不多

  src/App.vue 和 main.js做了更改，其余无变动

- backend文件夹：后端文件，初始化后，加入了控制后端接收文件的controller，和执行python代码逻辑的service

  前端上传的文件保存在uploads内，运行结果存在result内

- teacher文件夹：教师端发布代码的模拟，和学生代码的示例。

  evaluator为导入模型测评代码，backend也有一样的

  template为发布的模板代码，内含DQN和普通Q learning的实现

  student文件夹内含有模板的简单实现，可直接训练，也有训练结果的示例