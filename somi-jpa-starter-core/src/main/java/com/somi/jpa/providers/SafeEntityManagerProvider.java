package com.somi.jpa.providers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Proxy;
public class SafeEntityManagerProvider {
    private static final Logger log = LoggerFactory.getLogger(SafeEntityManagerProvider.class);
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
