import java.io.*;

public class YouTubeDownloader {

    // Método para baixar o áudio do YouTube
    public static void downloadAudioFromYouTube(String url, String outputFileBase) throws IOException {
        String outputFile = outputFileBase + ".wav";
        File wavFile = new File(outputFile);

        // Verifica se o arquivo já existe
        if (wavFile.exists()) {
            System.out.println("Arquivo já existe: " + outputFile);
            return; // Não baixa novamente
        }

        String ytDlpPath = new File("ExpoPlaylistMusical/resources/yt-dlp.exe").getAbsolutePath();
        String ffmpegPath = new File("ExpoPlaylistMusical/resources/ffmpeg/bin/ffmpeg.exe").getAbsolutePath();

        // Baixa direto como .wav
        String command = String.format("\"%s\" --ffmpeg-location \"%s\" -f bestaudio -x --audio-format wav -o \"%s.%%(ext)s\" \"%s\"",
            ytDlpPath, ffmpegPath, outputFileBase, url);

        System.out.println("Comando executado: " + command);

        Process process = Runtime.getRuntime().exec(command);

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        while ((line = reader.readLine()) != null) System.out.println(line);
        while ((line = errorReader.readLine()) != null) System.err.println(line);

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Erro ao baixar o áudio do YouTube.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IOException("Erro ao baixar o áudio: " + e.getMessage());
        }
    }
}
