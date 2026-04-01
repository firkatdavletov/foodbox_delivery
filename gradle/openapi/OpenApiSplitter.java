import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public final class OpenApiSplitter {
	private static final String MODE_PUBLIC = "public";
	private static final String MODE_ADMIN = "admin";
	private static final String COMPONENTS_REF_PREFIX = "#/components/";

	private OpenApiSplitter() {
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			throw new IllegalArgumentException(
				"Usage: OpenApiSplitter <public|admin> <input-openapi.yaml> <output-openapi.yaml>"
			);
		}

		String mode = args[0];
		Path input = Path.of(args[1]);
		Path output = Path.of(args[2]);

		ModeSettings settings = resolveMode(mode);
		LinkedHashMap<String, Object> source = loadYamlMap(input);
		LinkedHashMap<String, Object> filtered = buildFilteredSpec(source, settings);
		writeYamlMap(output, filtered);
	}

	private static ModeSettings resolveMode(String mode) {
		return switch (mode) {
			case MODE_PUBLIC -> new ModeSettings("Public API", false);
			case MODE_ADMIN -> new ModeSettings("Admin API", true);
			default -> throw new IllegalArgumentException("Unsupported mode: " + mode);
		};
	}

	private static boolean isAdminPath(String path) {
		return path.startsWith("/api/v1/admin/") || path.startsWith("/api/admin/");
	}

	private static LinkedHashMap<String, Object> loadYamlMap(Path input) throws IOException {
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
		Object loaded;
		try (Reader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
			loaded = yaml.load(reader);
		}

		if (!(loaded instanceof Map<?, ?> rootMap)) {
			throw new IllegalStateException("OpenAPI source file must contain a YAML object at root");
		}

		return castToLinkedHashMap(deepCopy(rootMap));
	}

	private static void writeYamlMap(Path output, Map<String, Object> content) throws IOException {
		Files.createDirectories(output.getParent());

		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		options.setIndent(2);
		options.setIndicatorIndent(1);
		options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);

		Yaml yaml = new Yaml(options);
		try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
			yaml.dump(content, writer);
		}
	}

	private static LinkedHashMap<String, Object> buildFilteredSpec(
		LinkedHashMap<String, Object> source,
		ModeSettings settings
	) {
		Map<?, ?> sourcePaths = asMap(source.get("paths"), "paths");
		LinkedHashMap<String, Object> filteredPaths = new LinkedHashMap<>();
		Set<String> usedTags = new LinkedHashSet<>();

		for (Map.Entry<?, ?> entry : sourcePaths.entrySet()) {
			if (entry.getKey() == null) {
				continue;
			}

			String path = entry.getKey().toString();
			boolean adminPath = isAdminPath(path);
			if (settings.adminOnly() != adminPath) {
				continue;
			}

			filteredPaths.put(path, deepCopy(entry.getValue()));

			if (entry.getValue() instanceof Map<?, ?> operations) {
				collectOperationTags(operations, usedTags);
			}
		}

		LinkedHashMap<String, Object> filteredComponents = buildFilteredComponents(source, filteredPaths);
		LinkedHashMap<String, Object> filteredSpec = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			switch (key) {
				case "paths" -> filteredSpec.put(key, filteredPaths);
				case "components" -> {
					if (!filteredComponents.isEmpty()) {
						filteredSpec.put(key, filteredComponents);
					}
				}
				case "tags" -> filteredSpec.put(key, filterTags(value, usedTags));
				case "info" -> filteredSpec.put(key, updateInfoTitle(value, settings.titleSuffix()));
				default -> filteredSpec.put(key, deepCopy(value));
			}
		}

		return filteredSpec;
	}

	private static LinkedHashMap<String, Object> buildFilteredComponents(
		LinkedHashMap<String, Object> source,
		LinkedHashMap<String, Object> filteredPaths
	) {
		if (!(source.get("components") instanceof Map<?, ?> sourceComponents)) {
			return new LinkedHashMap<>();
		}

		ReferenceCollector collector = new ReferenceCollector();
		ArrayDeque<ComponentPointer> queue = new ArrayDeque<>();
		collector.collect(buildReferenceRoots(source, filteredPaths), queue);

		Set<ComponentPointer> processed = new LinkedHashSet<>();
		while (!queue.isEmpty()) {
			ComponentPointer pointer = queue.removeFirst();
			if (!processed.add(pointer)) {
				continue;
			}

			Map<?, ?> section = asMap(
				sourceComponents.get(pointer.section()),
				"components/" + pointer.section()
			);
			Object component = section.get(pointer.name());
			if (component == null) {
				throw new IllegalStateException(
					"Referenced OpenAPI component not found: #/components/" +
					pointer.section() +
					"/" +
					pointer.name()
				);
			}

			collector.collect(component, queue);
		}

		LinkedHashMap<String, Object> filteredComponents = new LinkedHashMap<>();
		for (Map.Entry<?, ?> sectionEntry : sourceComponents.entrySet()) {
			if (sectionEntry.getKey() == null || !(sectionEntry.getValue() instanceof Map<?, ?> sectionMap)) {
				continue;
			}

			String sectionName = sectionEntry.getKey().toString();
			Set<String> requiredNames = collector.requiredNames(sectionName);
			if (requiredNames.isEmpty()) {
				continue;
			}

			LinkedHashMap<String, Object> filteredSection = new LinkedHashMap<>();
			for (Map.Entry<?, ?> componentEntry : sectionMap.entrySet()) {
				if (componentEntry.getKey() == null) {
					continue;
				}

				String componentName = componentEntry.getKey().toString();
				if (requiredNames.contains(componentName)) {
					filteredSection.put(componentName, deepCopy(componentEntry.getValue()));
				}
			}

			if (!filteredSection.isEmpty()) {
				filteredComponents.put(sectionName, filteredSection);
			}
		}

		return filteredComponents;
	}

	private static Map<String, Object> buildReferenceRoots(
		LinkedHashMap<String, Object> source,
		LinkedHashMap<String, Object> filteredPaths
	) {
		LinkedHashMap<String, Object> referenceRoots = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			String key = entry.getKey();
			if ("components".equals(key) || "tags".equals(key) || "info".equals(key)) {
				continue;
			}
			referenceRoots.put(key, "paths".equals(key) ? filteredPaths : entry.getValue());
		}
		return referenceRoots;
	}

	private static void collectOperationTags(Map<?, ?> operations, Set<String> usedTags) {
		for (Object operation : operations.values()) {
			if (!(operation instanceof Map<?, ?> operationMap)) {
				continue;
			}

			Object tagsValue = operationMap.get("tags");
			if (!(tagsValue instanceof List<?> tags)) {
				continue;
			}

			for (Object tag : tags) {
				if (tag != null) {
					usedTags.add(tag.toString());
				}
			}
		}
	}

	private static List<Object> filterTags(Object value, Set<String> usedTags) {
		if (!(value instanceof List<?> tags)) {
			return List.of();
		}

		List<Object> filteredTags = new ArrayList<>();
		for (Object tag : tags) {
			if (!(tag instanceof Map<?, ?> tagMap)) {
				continue;
			}

			Object tagName = tagMap.get("name");
			if (tagName != null && usedTags.contains(tagName.toString())) {
				filteredTags.add(deepCopy(tagMap));
			}
		}
		return filteredTags;
	}

	private static Object updateInfoTitle(Object value, String titleSuffix) {
		if (!(value instanceof Map<?, ?> infoMap)) {
			return deepCopy(value);
		}

		LinkedHashMap<String, Object> info = castToLinkedHashMap(deepCopy(infoMap));
		Object title = info.get("title");
		if (title != null) {
			String normalizedTitle = title.toString().trim();
			if (!normalizedTitle.isEmpty()) {
				info.put("title", normalizedTitle + " (" + titleSuffix + ")");
			}
		}
		return info;
	}

	private static Map<?, ?> asMap(Object value, String name) {
		if (value instanceof Map<?, ?> map) {
			return map;
		}
		throw new IllegalStateException("OpenAPI source file must contain top-level '" + name + "' object");
	}

	private static ComponentPointer parseComponentRef(String ref) {
		if (!ref.startsWith(COMPONENTS_REF_PREFIX)) {
			return null;
		}

		String remainder = ref.substring(COMPONENTS_REF_PREFIX.length());
		int separatorIndex = remainder.indexOf('/');
		if (separatorIndex <= 0 || separatorIndex == remainder.length() - 1) {
			throw new IllegalStateException("Unsupported OpenAPI component ref: " + ref);
		}

		String section = decodeJsonPointerToken(remainder.substring(0, separatorIndex));
		String name = decodeJsonPointerToken(remainder.substring(separatorIndex + 1));
		return new ComponentPointer(section, name);
	}

	private static String decodeJsonPointerToken(String token) {
		return token.replace("~1", "/").replace("~0", "~");
	}

	private static Object deepCopy(Object value) {
		if (value instanceof Map<?, ?> map) {
			LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (entry.getKey() != null) {
					copy.put(entry.getKey().toString(), deepCopy(entry.getValue()));
				}
			}
			return copy;
		}

		if (value instanceof List<?> list) {
			List<Object> copy = new ArrayList<>(list.size());
			for (Object item : list) {
				copy.add(deepCopy(item));
			}
			return copy;
		}

		return value;
	}

	@SuppressWarnings("unchecked")
	private static LinkedHashMap<String, Object> castToLinkedHashMap(Object value) {
		return (LinkedHashMap<String, Object>) value;
	}

	private record ComponentPointer(String section, String name) {
	}

	private record ModeSettings(String titleSuffix, boolean adminOnly) {
	}

	private static final class ReferenceCollector {
		private final Map<String, Set<String>> requiredComponents = new LinkedHashMap<>();

		private void collect(Object node, ArrayDeque<ComponentPointer> queue) {
			if (node instanceof Map<?, ?> map) {
				Object refValue = map.get("$ref");
				if (refValue instanceof String ref) {
					addRef(ref, queue);
				}

				for (Map.Entry<?, ?> entry : map.entrySet()) {
					if (entry.getKey() != null && "security".equals(entry.getKey().toString())) {
						collectSecuritySchemes(entry.getValue(), queue);
					}
					collect(entry.getValue(), queue);
				}
				return;
			}

			if (node instanceof List<?> list) {
				for (Object item : list) {
					collect(item, queue);
				}
			}
		}

		private Set<String> requiredNames(String section) {
			return requiredComponents.getOrDefault(section, Collections.emptySet());
		}

		private void addRef(String ref, ArrayDeque<ComponentPointer> queue) {
			ComponentPointer pointer = parseComponentRef(ref);
			if (pointer == null) {
				return;
			}
			addComponent(pointer, queue);
		}

		private void addComponent(ComponentPointer pointer, ArrayDeque<ComponentPointer> queue) {
			Set<String> componentNames = requiredComponents.computeIfAbsent(
				pointer.section(),
				unused -> new LinkedHashSet<>()
			);
			if (componentNames.add(pointer.name())) {
				queue.addLast(pointer);
			}
		}

		private void collectSecuritySchemes(Object security, ArrayDeque<ComponentPointer> queue) {
			if (!(security instanceof List<?> securityRequirements)) {
				return;
			}

			for (Object securityRequirement : securityRequirements) {
				if (!(securityRequirement instanceof Map<?, ?> requirementMap)) {
					continue;
				}

				for (Object schemeName : requirementMap.keySet()) {
					if (schemeName != null) {
						addComponent(
							new ComponentPointer("securitySchemes", schemeName.toString()),
							queue
						);
					}
				}
			}
		}
	}
}
