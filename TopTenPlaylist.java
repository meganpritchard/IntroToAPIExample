import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class TopTenPlaylist {
	private static final long NUMBER_OF_VIDEOS_RETURNED = 10;

	// Define a global instance of a Youtube object, which will be used to make
	// YouTube Data API requests.
	private static YouTube youtube;

	public static void main(String[] args) {
		// This OAuth 2.0 access scope allows for full read/write access to the
		// authenticated user's account.
		List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");
		try {
			// Authorize the request.
			Credential credential = Auth.authorize(scopes, "playlistupdates");

			// TODO 1: Create the client.
			youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
					.setApplicationName("TopTenPlaylist").build();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			String query = getUserInput();
			List<SearchResult> results = GetVideoIdList(query);
			CreatePlaylist(results, query);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getUserInput() {
		System.out.println("Please enter a topic to create a TopTenPLaylist: ");
		Scanner scanner = new Scanner(System.in);
		String query = scanner.nextLine();
		scanner.close();
		return query;
	}

	// TODO 3: Implement Search.
	public static List<SearchResult> GetVideoIdList(String queryTerm) throws IOException {
		// Define the API request for retrieving search results. We'll receive id and snippet for each result.
		YouTube.Search.List search = youtube.search().list("id,snippet");

		// Grabbing api_key from file.
		Scanner scanner = new Scanner(new File("api_key.txt"));
		String apiKey = scanner.nextLine();
		scanner.close();

		// Setting the request fields.
		search.setKey(apiKey);
		search.setQ(queryTerm);
		search.setType("video");
		search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
		search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

		// Call the API and return the results.
		SearchListResponse searchResponse = search.execute();
		List<SearchResult> searchResultList = searchResponse.getItems();
		return searchResultList;
	}

	// TODO 4: Create a playlist.
	public static void CreatePlaylist(List<SearchResult> items, String query) throws IOException {
		String playlistId = insertPlaylist(query); // Implement these.

		// If a valid playlist was created, add a video to that playlist.
		for (SearchResult item : items) {
			insertPlaylistItem(playlistId, item.getId(), item.getSnippet().getTitle()); // Implement these
		}
	}

	// TODO 5: Add to Playlist.
	// This adds an item to a playlist, given the playlistId and the resourceId and title
	// of the item being added.
	private static void insertPlaylistItem(String playlistId, ResourceId id, String title)
			throws IOException {
		// Set fields included in the playlistItem resource's "snippet" part.
		PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
		playlistItemSnippet.setTitle(title); 
		playlistItemSnippet.setPlaylistId(playlistId);
		playlistItemSnippet.setResourceId(id);

		// Create the playlistItem and set its snippet.
		PlaylistItem playlistItem = new PlaylistItem();
		playlistItem.setSnippet(playlistItemSnippet);

		// Creating the request to insert an item. Here, the first argument identifies
		// the resource parts that the API response should contain, and the second
		//argument is the playlist item being inserted.
		YouTube.PlaylistItems.Insert playlistItemsInsertCommand = youtube.playlistItems()
				.insert("snippet,contentDetails", playlistItem);

		// Commits the call to insert into playlist.
		playlistItemsInsertCommand.execute();
	}

	// This creates a TopTenPlaylist based on the input the user gave us.
	public static String insertPlaylist(String query) throws IOException {
		// This code constructs the playlist resource that is being inserted.
		// It sets the playlist's title, description, and privacy status.
		PlaylistSnippet playlistSnippet = new PlaylistSnippet();
		playlistSnippet.setTitle("Top Ten " + query);
		playlistSnippet.setDescription("A playlist containing the top ten videos of " + query);

		PlaylistStatus playlistStatus = new PlaylistStatus();
		playlistStatus.setPrivacyStatus("public"); 

		// Create a new playlist and add the snippet and status we just created.
		Playlist youTubePlaylist = new Playlist();
		youTubePlaylist.setSnippet(playlistSnippet);
		youTubePlaylist.setStatus(playlistStatus);

		// Create the request to insert the playlist.
		YouTube.Playlists.Insert playlistInsertCommand;
		Playlist playlistInserted;
		// In this call, the first specifies what the response should contain and the second
		// is the playlist being inserted with the previosult set snippet and status.
		playlistInsertCommand = youtube.playlists().insert("snippet,status", youTubePlaylist);
		playlistInserted = playlistInsertCommand.execute();

		// Return the id of the inserted playlist.
		return playlistInserted.getId();
	}

	public static class Auth {
		// Define a global instance of the HTTP transport.
		public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

		// Define a global instance of the JSON factory.
		public static final JsonFactory JSON_FACTORY = new JacksonFactory();

		// This is the directory that will be used under the user's home
		// directory where OAuth tokens will be stored.
		private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials";

		/**
		 * Authorizes the installed application to access user's protected data.
		 *
		 * @param scopes
		 *            list of scopes needed to run youtube upload.
		 * @param credentialDatastore
		 *            name of the credential datastore to cache OAuth tokens
		 */
		// TODO 2: Set up the AUTH.
		public static Credential authorize(List<String> scopes, String credentialDatastore) throws IOException {
			// Getting the client_id and client_secret from a file.
			Scanner scanner = new Scanner(new File("app_creds.txt"));
			String client_id = scanner.nextLine();
			String client_secret = scanner.nextLine();
			scanner.close();
			
			// Setting up the web details for the client secrets.
			GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
			details.setAuthUri("https://accounts.google.com/o/oauth2/auth");
			details.setClientId(client_id);
			details.setClientSecret(client_secret);
			details.setRedirectUris(Lists.newArrayList("http://localhost:8080/Callback"));
			details.setTokenUri("https://accounts.google.com/o/oauth2/token");

			// Adding the web details to the client secrets.
			GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
			clientSecrets.setWeb(details);

			// This creates the credentials datastore at ~/.oauth-credentials/${credentialDatastore}
			FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(
					new File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY));
			DataStore<StoredCredential> datastore = fileDataStoreFactory.getDataStore(credentialDatastore);

			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
					clientSecrets, scopes).setCredentialDataStore(datastore).build();

			// Build the local server and bind it to port 8080. This is where we get the redirect URI.
			LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(8080).build();

			// Authorize.
			return new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user");
		}
	}
}
