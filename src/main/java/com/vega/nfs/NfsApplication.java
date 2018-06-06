package com.vega.nfs;

import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http11.Http11AprProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
//@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
public class NfsApplication {

	@Value("${tomcat.apr:false}")
	private boolean enabled;

	public static void main(String[] args) {
		SpringApplication.run(NfsApplication.class, args);
	}

	//Tomcat large file upload connection reset
	@Bean
	public TomcatEmbeddedServletContainerFactory tomcatEmbedded() {

		TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();

		if (enabled) {
			LifecycleListener arpLifecycle = new AprLifecycleListener();
			tomcat.setProtocol("org.apache.coyote.http11.Http11AprProtocol");
			tomcat.addContextLifecycleListeners(arpLifecycle);
		}
		// 设置默认端口号
		// tomcat.setPort(8080);
		tomcat.addConnectorCustomizers((TomcatConnectorCustomizer) connector -> {
			if ((connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?>)) {
				//-1 means unlimited
				((AbstractHttp11Protocol<?>) connector.getProtocolHandler()).setMaxSwallowSize(-1);
				if (enabled) {
					Http11AprProtocol protocol = (Http11AprProtocol) connector.getProtocolHandler();
					//设置最大连接数
					protocol.setMaxConnections(2000);
					//设置最大线程数
					protocol.setMaxThreads(2000);
					protocol.setConnectionTimeout(30000);
				} else {
					Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
					protocol.setMaxConnections(2000);
					protocol.setMaxThreads(2000);
					protocol.setConnectionTimeout(30000);
				}
			}
		});
		return tomcat;
	}
}
