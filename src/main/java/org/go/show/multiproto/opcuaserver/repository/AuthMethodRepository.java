package org.go.show.multiproto.opcuaserver.repository;

import org.go.show.multiproto.opcuaserver.entity.AuthMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 认证方法配置仓库
 */
@Repository
public interface AuthMethodRepository extends JpaRepository<AuthMethodEntity, Long> {

    /**
     * 根据方法名查找认证方法配置
     */
    Optional<AuthMethodEntity> findByMethodName(String methodName);

    /**
     * 检查指定方法名的配置是否存在
     */
    boolean existsByMethodName(String methodName);
}
