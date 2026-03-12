package com.breadcost.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, String> {

    List<NotificationTemplateEntity> findByTenantId(String tenantId);

    List<NotificationTemplateEntity> findByTenantIdAndType(String tenantId, String type);

    List<NotificationTemplateEntity> findByTenantIdAndActiveTrue(String tenantId);

    Optional<NotificationTemplateEntity> findByTenantIdAndTypeAndChannel(
            String tenantId, String type, String channel);
}
