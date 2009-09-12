package de.faustedition.util;

import org.springframework.core.io.Resource;

public class ResourceUtil {
	public static Resource chooseExistingResource(Resource[] candidates) {
		for (Resource candidate : candidates) {
			if (candidate.exists() && candidate.isReadable()) {
				return candidate;
			}
		}
		return null;
	}
}
