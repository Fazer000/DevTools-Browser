package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
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
                        Row(
                            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "<${inspectedElement.tagName}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CodeTag,
                                fontSize = 13.sp,
                                softWrap = false,
                                maxLines = 1
                            )
                            if (inspectedElement.id.isNotBlank()) {
                                Text(
                                    text = "#${inspectedElement.id}",
                                    fontFamily = FontFamily.Monospace,
                                    color = CodeAttr,
                                    fontSize = 13.sp,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                            if (inspectedElement.className.isNotBlank()) {
                                Text(
                                    text = ".${inspectedElement.className.replace(" ", ".")}",
                                    fontFamily = FontFamily.Monospace,
                                    color = CodeString,
                                    fontSize = 12.sp,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = ">",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CodeTag,
                                fontSize = 13.sp,
                                softWrap = false,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${inspectedElement.width.toInt()} × ${inspectedElement.height.toInt()} px",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Copy Action Buttons for Inspected Element
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(inspectedElement.outerHtml))
                                Toast.makeText(context, "Copied outerHTML", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy outerHTML", fontSize = 10.sp)
                        }

                        if (inspectedElement.innerHtml.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(inspectedElement.innerHtml))
                                    Toast.makeText(context, "Copied innerHTML", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy innerHTML", fontSize = 10.sp)
                            }
                        }

                        if (inspectedElement.selectorPath.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(inspectedElement.selectorPath))
                                    Toast.makeText(context, "Copied CSS Selector", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Copy Selector", fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = false,
                            maxLines = 1
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
                                    Text(text = "$k:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CodeAttr, softWrap = false)
                                    Text(text = v, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CodeText, softWrap = false)
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
                val horizontalScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("dom_tree_list")
                    ) {
                        item {
                            DomNodeItem(
                                node = domTree,
                                indent = 0,
                                targetSelectorPath = inspectedElement?.selectorPath,
                                inspectedElement = inspectedElement
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DomNodeItem(
    node: DomNode,
    indent: Int,
    targetSelectorPath: String?,
    inspectedElement: InspectedElement?
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(indent < 2) }

    LaunchedEffect(targetSelectorPath, inspectedElement) {
        if (!targetSelectorPath.isNullOrBlank() && node.containsSelector(targetSelectorPath)) {
            isExpanded = true
        }
    }

    val isInspectedTarget = remember(node, targetSelectorPath, inspectedElement) {
        if (node.isTextNode) false
        else if (!targetSelectorPath.isNullOrBlank() && node.selectorPath.isNotBlank() && node.selectorPath == targetSelectorPath) {
            true
        } else if (inspectedElement != null && !targetSelectorPath.isNullOrBlank()) {
            val nodeTagMatches = node.tagName.equals(inspectedElement.tagName, ignoreCase = true)
            val nodeIdMatches = inspectedElement.id.isBlank() || node.attributes["id"] == inspectedElement.id
            nodeTagMatches && nodeIdMatches && node.selectorPath.isNotBlank() && targetSelectorPath.contains(node.selectorPath)
        } else false
    }

    val rowBackground = if (isInspectedTarget) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    val rowBorder = if (isInspectedTarget) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    } else null

    Column(
        modifier = Modifier.padding(start = (indent * 12).dp)
    ) {
        Surface(
            color = rowBackground,
            border = rowBorder,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .padding(vertical = 1.dp)
                .clickable { isExpanded = !isExpanded }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (node.children.isNotEmpty()) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = if (isInspectedTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(14.dp))
                }

                if (isInspectedTarget) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "INSPECTED",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (node.isTextNode) {
                    Text(
                        text = "\"${node.textContent}\"",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CodeString,
                        softWrap = false,
                        maxLines = 1
                    )
                } else {
                    Text(text = "<", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CodeTag, softWrap = false)
                    Text(
                        text = node.tagName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isInspectedTarget) MaterialTheme.colorScheme.primary else CodeTag,
                        softWrap = false
                    )

                    // Attributes
                    node.attributes.forEach { (key, value) ->
                        Text(text = " $key=", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CodeAttr, softWrap = false)
                        Text(
                            text = "\"$value\"",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = CodeString,
                            softWrap = false
                        )
                    }

                    Text(
                        text = if (node.children.isEmpty()) " />" else ">",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CodeTag,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Copy node button
                IconButton(
                    onClick = {
                        val textToCopy = if (node.isTextNode) {
                            node.textContent
                        } else {
                            val attrsStr = node.attributes.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }
                            if (attrsStr.isNotBlank()) "<${node.tagName} $attrsStr>" else "<${node.tagName}>"
                        }
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        Toast.makeText(context, "Copied element tag", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Element",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Children
        if (isExpanded && node.children.isNotEmpty()) {
            node.children.forEach { childNode ->
                DomNodeItem(
                    node = childNode,
                    indent = indent + 1,
                    targetSelectorPath = targetSelectorPath,
                    inspectedElement = inspectedElement
                )
            }
            // Closing Tag
            if (!node.isTextNode) {
                Row(
                    modifier = Modifier
                        .padding(start = 14.dp, top = 1.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "</${node.tagName}>",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CodeTag,
                        softWrap = false
                    )
                }
            }
        }
    }
}
