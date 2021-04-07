package fr.gouv.clea.integrationtests.config;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Value
@Validated
@AllArgsConstructor
@ConstructorBinding
@ConfigurationProperties("clea")
public class ApplicationProperties {

    @Valid
    CleaWsRestProperties wsRest;

    @Valid
    CleaBatchProperties batch;

    @NotNull
    Integer dupScanThreshold;

    @NotNull
    String qrCodePrefix;

    @Nullable
    String serverAuthorityPublicKey;

    @Nullable
    String manualContactTracingAuthorityPublicKey;
}
