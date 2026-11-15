package dev.electricsteve.unpluggedPaper.commands;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import dev.electricsteve.unpluggedPaper.UnpluggedPaper;
import dev.electricsteve.unpluggedPaper.config.ConfigInstanceField;
import kotlin.Pair;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

public class ConfigurationVariableParameterType implements ParameterType<BukkitCommandActor, ConfigInstanceField> {

    private final UnpluggedPaper plugin;

    public ConfigurationVariableParameterType(UnpluggedPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public ConfigInstanceField parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<BukkitCommandActor> context) {
        String fieldName = input.readString();
        String[] parts;
        if (fieldName.contains(".")) {
            parts = fieldName.split("\\.");
        } else {
            parts = new String[]{fieldName};
        }
        Object instance = plugin.getConfiguration();
        Class<?> instanceClass = instance.getClass();
        Field field;
        List<Pair<Object, Field>> list = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            try {
                field = instanceClass.getDeclaredField(part);
            } catch (NoSuchFieldException _) {
                throw new CommandErrorException("Invalid config path");
            }
            list.add(new Pair<>(instance, field));
            if (i != parts.length - 1) {
                try {
                    field.setAccessible(true);
                    instance = field.get(instance);
                    instanceClass = instance.getClass();
                } catch (IllegalAccessException e) {
                    throw new CommandErrorException("Error getting this field");
                }
            }
        }
        return new ConfigInstanceField(list.reversed(), fieldName);
    }

    @Override
    public @NotNull SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
        return _ -> getFieldNames(plugin.getConfiguration().getClass());
    }

    private List<String> getFieldNames(Class<?> clazz) {
        List<String> list = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            String fieldName = field.getName();
            if (fieldType.getPackageName().startsWith("dev.electricsteve")) {
                list.addAll(getFieldNames(fieldType).stream().map(string -> fieldName + "." + string).toList());
            } else {
                list.add(fieldName);
            }
        }
        return list;
    }
}
