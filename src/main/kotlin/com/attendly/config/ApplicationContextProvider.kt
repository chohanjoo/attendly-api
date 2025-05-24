package com.attendly.config

import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/**
 * 애플리케이션 컨텍스트에 접근할 수 있는 유틸리티 컴포넌트
 * 실행 환경 외부(Logback 등)에서 스프링 빈에 접근해야 할 때 사용
 */
@Component
class ApplicationContextProvider : ApplicationContextAware {
    companion object {
        private var applicationContext: ApplicationContext? = null

        fun getApplicationContext(): ApplicationContext? {
            return applicationContext
        }

        fun getBean(beanName: String): Any? {
            return applicationContext?.getBean(beanName)
        }

        fun <T> getBean(beanClass: Class<T>): T? {
            return applicationContext?.getBean(beanClass)
        }
    }

    @Throws(BeansException::class)
    override fun setApplicationContext(context: ApplicationContext) {
        applicationContext = context
    }
} 