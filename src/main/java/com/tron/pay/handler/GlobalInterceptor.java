package com.tron.pay.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.PrintWriter;

@Component
public class GlobalInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 在这里可以添加全局的请求处理逻辑，例如用户认证、权限检查等
        if (StpUtil.isLogin()) {
            return true;
        } else {
            // 设置响应类型和编码
            response.setContentType("application/json;charset=UTF-8");
            // 创建返回信息
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", 401);
            jsonObject.put("message", "未授权，请登录");
            // 输出返回信息
            PrintWriter out = response.getWriter();
            out.write(jsonObject.toString());
            out.flush();
            out.close();
            return false;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 在这里可以添加全局的响应处理逻辑，例如处理异常、添加响应头等
//        System.out.println("postHandle: 请求处理之后调用，但在视图渲染之前");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 在这里可以添加全局的响应处理逻辑，例如日志记录、资源清理等
//        System.out.println("afterCompletion: 视图渲染之后调用");
    }
}