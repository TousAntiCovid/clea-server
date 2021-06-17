package fr.gouv.clea.integrationtests.config;

import lombok.AllArgsConstructor;
import lombok.Value;

import javax.validation.Valid;

import java.util.List;

@Value
@Valid
@AllArgsConstructor
public class CleaBatchProperties {

    List<String> command;

    int timeoutInSeconds;
}
