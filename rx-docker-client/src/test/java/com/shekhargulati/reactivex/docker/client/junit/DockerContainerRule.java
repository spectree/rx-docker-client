package com.shekhargulati.reactivex.docker.client.junit;

import com.shekhargulati.reactivex.docker.client.DockerClient;
import com.shekhargulati.reactivex.docker.client.representations.DockerContainerRequest;
import com.shekhargulati.reactivex.docker.client.representations.DockerContainerRequestBuilder;
import com.shekhargulati.reactivex.docker.client.representations.DockerContainerResponse;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class DockerContainerRule implements TestRule {

    private final DockerClient client;
    private List<String> containerIds;

    public DockerContainerRule(DockerClient client) {
        this.client = client;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        try {
            CreateDockerContainer[] containerAnnotations = description.getTestClass().getDeclaredMethod(description.getMethodName()).getAnnotationsByType(CreateDockerContainer.class);
            if (containerAnnotations != null && containerAnnotations.length > 0) {
                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        containerIds = createContainers(Stream.of(containerAnnotations).map(c -> c.container()));
                        try {
                            base.evaluate();
                        } finally {
                            containerIds.forEach(DockerContainerRule.this::removeContainer);
                        }
                    }
                };
            }
            return base;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> createContainers(Stream<String> containers) {
        return containers.map(this::createContainer).map(DockerContainerResponse::getId).collect(toList());
    }

    private DockerContainerResponse createContainer(String containerName) {
        DockerContainerRequest request = new DockerContainerRequestBuilder()
                .setImage("ubuntu")
                .setCmd(Collections.singletonList("/bin/bash"))
                .setAttachStdin(true)
                .setTty(true)
                .createDockerContainerRequest();
        return client.createContainer(request, containerName);

    }


    private void removeContainer(String containerId) {
        try {
            client.removeContainer(containerId, false, true);
        } catch (Exception e) {
            // ignore as circle ci does not allow containers and images to be destroyed
        }
    }

    public List<String> containerIds() {
        return containerIds;
    }
}
