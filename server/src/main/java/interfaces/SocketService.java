package interfaces;

import java.util.HashMap;

public interface SocketService 
{
    //account
    public Object addAccount(HashMap<String, Object> argsTable) throws Exception;
    public Object getAccount(HashMap<String, Object> argsTable) throws Exception; 
    public Object deleteAccount(HashMap<String, Object> argsTable) throws Exception;

    //song
    public Object getMostPopularSongs(HashMap<String, Object> argsTable) throws Exception;
    public Object searchSongs(HashMap<String, Object> argsTable) throws Exception;
    public Object getSongByIDs(HashMap<String, Object> argsTable) throws Exception;
    public Object getAlbumsSongs(HashMap<String, Object> argsTable) throws Exception;
    public Object getArtistSongs(HashMap<String, Object> argsTable) throws Exception;
    public Object getPlaylistSongs(HashMap<String, Object> argsTable) throws Exception;

    //album
    public Object getRecentPublischedAlbum(HashMap<String, Object> argsTable) throws Exception;
    public Object searchAlbums(HashMap<String, Object> argsTable) throws Exception;
    public Object getAlbumByID(HashMap<String, Object> argsTable) throws Exception; 
    public Object getArtistsAlbums(HashMap<String, Object> argsTable) throws Exception;
    
    //artisti
    public Object searchArtists(HashMap<String, Object> argsTable) throws Exception;
    public Object getArtistsByIDs(HashMap<String, Object> argsTable) throws Exception;

    //playlist
    public Object addPlaylist(HashMap<String, Object> argsTable) throws Exception;
    public Object deletePlaylist(HashMap<String, Object> argsTable) throws Exception;
    public Object removeSongFromPlaylist(HashMap<String, Object> argsTable) throws Exception;
    public Object addSongToPlaylist(HashMap<String, Object> argsTable) throws Exception;
    public Object getAccountsPlaylists(HashMap<String, Object> argsTable) throws Exception;
    public Object renamePlaylist(HashMap<String, Object> argsTable) throws Exception;

    //Comment
    public Object getAccountSongComment(HashMap<String, Object> argsTable) throws Exception;
    public Object addComment(HashMap<String, Object> argsTable) throws Exception;
    public Object deleteComment(HashMap<String, Object> argsTable) throws Exception;
    public Object getSongComment(HashMap<String, Object> argsTable) throws Exception;
    public Object getAccountComment(HashMap<String, Object> argsTable) throws Exception;

    //emozioni
    public Object getSongEmotion(HashMap<String, Object> argsTable) throws Exception;
    
    
}
