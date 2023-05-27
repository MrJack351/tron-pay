# TRON-PAY

基于TRON链开发的trc20支付系统

# 功能

1. 已实现订单回调功能，
2. 订单创建后会每隔5s扫描监听地址的交易记录，
3. 采用线程池方式，其它功能二次开发即可
4. 系统目前只写了trc20收款，未来或许会支持eth

# 运行环境

本项目采用前后端一体，前端基于bootstrap4开发的光年模板开发

后端：idea2022.3.3，maven3.6，jdk8，spring boot2.5.2，sa-token，mybatisplus

前端：thymeleaf，bootstrap，jQuery，js

[光年模板使用文档](http://www.bixiaguangnian.com/)

# 快速开始

1. 导入sql文件
2. 配置数据库信息
3. 配置baseUrl
4. 运行项目即可

# 系统演示

登录

![](.\image\Snipaste_2023-05-27_13-25-19.png)

首页

![](.\image\Snipaste_2023-05-27_13-26-05.png)

订单

![](.\image\Snipaste_2023-05-27_13-26-24.png)

订单统计

![](.\image\Snipaste_2023-05-27_13-26-31.png)

商户

![](.\image\Snipaste_2023-05-27_13-26-36.png)

收银台

![](.\image\Snipaste_2023-05-27_13-27-04.png)

支付成功

![](.\image\Snipaste_2023-05-27_13-27-10.png)

接入文档

![](.\image\Snipaste_2023-05-27_13-26-53.png)
