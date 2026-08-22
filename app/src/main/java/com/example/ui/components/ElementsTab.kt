package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DomNode
import com.example.data.InspectedElement
import com.example.ui.theme.CodeAttr
import com.example.ui.theme.CodeString
import com.example.ui.theme.CodeTag
import com.example.ui.theme.CodeText

@Composable
fun ElementsTab(
    domTree: DomNode?,
    inspectedElement: InspectedElement?,
    onToggleInspector: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStylesSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Inspected Element Banner
        if (inspectedElement != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "<${inspectedElement.tagName}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CodeTag,
                                fontSize = 14.sp
                            )
                            if (inspectedElement.id.isNotBlank()) {
                                Text(
                                    text = "#${inspectedElement.id}",
                                    fontFamily = FontFamily.Monospace,
                                    color = CodeAttr,
                                    fontSize = 14.sp
                                )
                            }
                            if (inspectedElement.className.isNotBlank()) {
                                Text(
                                    text = ".${inspectedElement.className.replace(" ", ".")}",
                                    fontFamily = FontFamily.Monospace,
                                    color = CodeString,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = ">",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CodeTag,
                                fontSize = 14.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${inspectedElement.width.toInt()} × ${inspectedElement.height.toInt()} px",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Computed Styles Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Color: ${inspectedElement.computedStyles["color"] ?: "-"} | Display: ${inspectedElement.computedStyles["display"] ?: "-"}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = { showStylesSheet = !showStylesSheet },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showStylesSheet) "Hide Styles" else "View Styles & Box Model",
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Expanded Computed Styles & Box Model
                    AnimatedVisibility(visible = showStylesSheet) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Computed CSS Styles",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            inspectedElement.computedStyles.forEach { (k, v) ->
                                Row(
                                    modifier = Modifier.padding(vertical = 1.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "$k:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CodeAttr)
                                    Text(text = v, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CodeText)
                                }
                            }
                        }
                    }
                }
            }
        }

        // DOM Tree
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DOM Tree Hierarchy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = onToggleInspector,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Select Element On Page", fontSize = 11.sp)
                }
            }

            if (domTree == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "DOM Tree is loading or unavailable...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("dom_tree_list")
                ) {
                    item {
                        DomNodeItem(node = domTree, indent = 0)
                    }
                }
            }
        }
    }
}

@Composable
fun DomNodeItem(
    node: DomNode,
    indent: Int
) {
    var isExpanded by remember { mutableStateOf(indent < 2) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 12).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.children.isNotEmpty()) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(14.dp))
            }

            if (node.isTextNode) {
                Text(
                    text = "\"${node.textContent}\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = CodeString
                )
            } else {
                Text(text = "<", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CodeTag)
                Text(
                    text = node.tagName,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CodeTag
                )

                // Attributes
                node.attributes.forEach { (key, value) ->
                    Text(text = " $key=", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CodeAttr)
                    Text(
                        text = "\"${if (value.length > 25) value.take(25) + "..." else value}\"",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CodeString
                    )
                }

                Text(
                    text = if (node.children.isEmpty()) " />" else ">",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = CodeTag
                )
            }
        }

        // Children
        if (isExpanded && node.children.isNotEmpty()) {
            node.children.forEach { childNode ->
                DomNodeItem(node = childNode, indent = indent + 1)
            }
            // Closing Tag
            if (!node.isTextNode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 1.dp, bottom = 1.dp)
                ) {
                    Text(text = "</${node.tagName}>", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CodeTag)
                }
            }
        }
    }
}
