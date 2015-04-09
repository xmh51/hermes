package com.jlfex.hermes.main.cfca.init;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import com.jlfex.hermes.common.Logger;

import cfca.payment.api.system.PaymentEnvironment;

public class CFCAPaymentInitListener implements ServletContextListener {

	private static final String PAYMENT_CONFIG_PATH = "payment";

	public void contextInitialized(ServletContextEvent servletContextEvent) {
		Logger.info("开始初始化中金支付环境了");
		try {
			// 初始化支付环境
			String path = getClass().getClassLoader().getResource("/").getPath() + PAYMENT_CONFIG_PATH;
			Logger.info("中金配置文件路径："+path);
			PaymentEnvironment.initialize(path);
		} catch (Exception e) {
			Logger.error("初始化中金支付环境异常:", e);
		}
		Logger.info("初始化中金支付环境完毕");
	}

	public void contextDestroyed(ServletContextEvent servletContextEvent) {

	}
}
