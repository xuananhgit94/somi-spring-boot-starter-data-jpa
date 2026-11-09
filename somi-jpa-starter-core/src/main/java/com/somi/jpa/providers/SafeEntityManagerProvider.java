package com.somi.jpa.providers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Proxy;
@Slf4j
public class SafeEntityManagerProvider {
    @PersistenceContext
    private EntityManager entityManager;
    public EntityManager getEntityManager() {
        if (!isSafeProxy(entityManager)) {
            log.warn("EntityManager is NOT transactional-scoped! You may be outside @Transactional context.");
        }
        return entityManager;
    }
    private boolean isSafeProxy(EntityManager em) {
        return AopUtils.isAopProxy(em) || Proxy.isProxyClass(em.getClass());
    }
}
