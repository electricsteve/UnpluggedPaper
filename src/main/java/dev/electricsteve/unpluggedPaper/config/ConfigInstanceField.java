package dev.electricsteve.unpluggedPaper.config;

import java.lang.reflect.Field;
import java.util.List;

import kotlin.Pair;

/// The list is sorted from the actual field to the root
public record ConfigInstanceField(List<Pair<Object, Field>> list) {

}
