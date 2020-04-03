package com.artipie.npm;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Optional;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Meta}.
 *
 * @since 0.1
 */
public final class MetaTest {

    /**
     * Dist tags json attribute name.
     */
    private static final String DISTTAGS = "dist-tags";

    @Test
    public void canUpdateMetaDistTags() {
        MatcherAssert.assertThat(
            new Meta(
                Json.createObjectBuilder()
                .add("versions",
                    Json.createObjectBuilder().add(
                        MetaTest.DISTTAGS,
                        Json.createObjectBuilder().add("latest", "1.0.0")
                    )
                ).build(),
                Optional.empty()
            ).updatedMeta(
                Json.createObjectBuilder()
                    .add("versions",
                        Json.createObjectBuilder().add(
                            MetaTest.DISTTAGS,
                            Json.createObjectBuilder().add("alpha", "1.0.1")
                        )
                    ).build()
            )
            .byteFlow()
            .concatMap(
                    buffer -> Flowable.just(buffer.array())
                ).reduce(
                (arr1, arr2) ->
                    ByteBuffer.wrap(
                        new byte[arr1.length + arr2.length]
                    ).put(arr1).put(arr2).array()
            ).blockingGet(),
            new IsEqual<>(
                Json.createObjectBuilder()
                    .add(
                        MetaTest.DISTTAGS,
                        Json.createObjectBuilder().add("latest", "1.0.0").add("alpha", "1.0.1")
                    )
                    .build()
            )
        );
    }
}
