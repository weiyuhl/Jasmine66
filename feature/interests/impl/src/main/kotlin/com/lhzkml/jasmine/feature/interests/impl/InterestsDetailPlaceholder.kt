
package com.lhzkml.jasmine.feature.interests.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhzkml.jasmine.core.designsystem.theme.JasmineTheme
import com.lhzkml.jasmine.feature.interests.api.R

@Composable
fun InterestsDetailPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                20.dp,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.feature_interests_api_ic_detail_placeholder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(id = R.string.feature_interests_api_select_an_interest),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Preview(widthDp = 200, heightDp = 300)
@Composable
fun TopicDetailPlaceholderPreview() {
    JasmineTheme {
        InterestsDetailPlaceholder()
    }
}


