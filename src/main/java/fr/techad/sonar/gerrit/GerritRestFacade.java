package fr.techad.sonar.gerrit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import fr.techad.sonar.GerritPluginException;

public class GerritRestFacade extends GerritFacade {
	private static final Logger LOG = Loggers.get(GerritRestFacade.class);
	private static final String JSON_RESPONSE_PREFIX = ")]}'";
	private static final String COMMIT_MSG = "/COMMIT_MSG";
	private static final String ERROR_LISTING = "Error listing files";
	private static final String ERROR_SETTING = "Error setting review";
	private final GerritConnector gerritConnector;
	private List<String> gerritFileList = new ArrayList<String>();

	public GerritRestFacade(GerritConnectorFactory gerritConnectorFactory) {
		LOG.debug("[GERRIT PLUGIN] Instanciating GerritRestFacade");
		this.gerritConnector = gerritConnectorFactory.getConnector();
	}

	@NotNull
	@Override
	public List<String> listFiles() throws GerritPluginException {
		if (!gerritFileList.isEmpty()) {
			LOG.debug("[GERRIT PLUGIN] File list already filled. Not calling Gerrit.");
		} else {
			try {
				String rawJsonString = gerritConnector.listFiles();
				String jsonString = trimResponse(rawJsonString);
				JsonElement rootJsonElement = new JsonParser().parse(jsonString);
				for (Entry<String, JsonElement> fileList : rootJsonElement.getAsJsonObject().entrySet()) {
					String fileName = fileList.getKey();
					if (COMMIT_MSG.equals(fileName)) {
						continue;
					}
					gerritFileList.add(fileName);
				}
			} catch (IOException e) {
				throw new GerritPluginException(ERROR_LISTING, e);
			}
		}
		return Collections.unmodifiableList(gerritFileList);
	}

	@Override
	public void setReview(@NotNull ReviewInput reviewInput) throws GerritPluginException {
		try {
			gerritConnector.setReview(formatReview(reviewInput));
		} catch (IOException e) {
			throw new GerritPluginException(ERROR_SETTING, e);
		}
	}

	@NotNull
	protected String trimResponse(@NotNull String response) {
		return StringUtils.replaceOnce(response, JSON_RESPONSE_PREFIX, "");
	}
}
