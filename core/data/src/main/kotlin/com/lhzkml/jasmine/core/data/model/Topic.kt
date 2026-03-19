
package com.lhzkml.jasmine.core.data.model

import com.lhzkml.jasmine.core.database.model.TopicEntity
import com.lhzkml.jasmine.core.network.model.NetworkTopic

fun NetworkTopic.asEntity() = TopicEntity(
    id = id,
    name = name,
    shortDescription = shortDescription,
    longDescription = longDescription,
    url = url,
    imageUrl = imageUrl,
)

