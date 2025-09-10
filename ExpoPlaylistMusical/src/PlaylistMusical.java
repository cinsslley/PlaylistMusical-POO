import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class PlaylistMusical {

    private static boolean modoEscuro = false;
    private static Clip clipAtual = null;

    private static final File ARQUIVO_PLAYLIST = new File("playlist.txt");
    private static final File ARQUIVO_FAVORITOS = new File("favoritos.txt");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Playlist playlistPrincipal = new Playlist("Minha Playlist");
            Playlist favoritos = new Playlist("Favoritos");

            carregarPlaylist(playlistPrincipal, ARQUIVO_PLAYLIST);
            carregarPlaylist(favoritos, ARQUIVO_FAVORITOS);

            criarInterface(playlistPrincipal, favoritos);
        });
    }

    private static void criarInterface(Playlist playlistPrincipal, Playlist favoritos) {
        DefaultListModel<Musica> modeloPlaylist = new DefaultListModel<>();
        playlistPrincipal.musicas.forEach(modeloPlaylist::addElement);

        DefaultListModel<Musica> modeloFavoritos = new DefaultListModel<>();
        favoritos.musicas.forEach(modeloFavoritos::addElement);

        JFrame frame = new JFrame("Playlist Musical");
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane abas = new JTabbedPane();
        abas.setFont(new Font("Monospace", Font.BOLD, 14));

        abas.addTab(playlistPrincipal.nome, criarPainelPlaylist(playlistPrincipal, modeloPlaylist, favoritos, modeloFavoritos));
        abas.addTab(favoritos.nome, criarPainelFavoritos(favoritos, modeloFavoritos));

        JButton btnTema = new JButton("Ativar Modo Escuro");
        btnTema.addActionListener(e -> {
            modoEscuro = !modoEscuro;
            btnTema.setText(modoEscuro ? "Desativar Modo Escuro" : "Ativar Modo Escuro");
            aplicarTema(frame.getContentPane(), modoEscuro);
        });

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topo.add(btnTema);

        JPanel principal = new JPanel(new BorderLayout());
        principal.add(topo, BorderLayout.NORTH);
        principal.add(abas, BorderLayout.CENTER);

        aplicarTema(principal, modoEscuro);
        frame.setContentPane(principal);
        frame.setVisible(true);
    }

    private static JPanel criarPainelPlaylist(Playlist playlist, DefaultListModel<Musica> modeloPlaylist,
                                              Playlist favoritos, DefaultListModel<Musica> modeloFavoritos) {

        JList<Musica> lista = new JList<>(modeloPlaylist);
        lista.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lista.setFixedCellHeight(28);

        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        painel.add(new JLabel("Playlist: " + playlist.nome, SwingConstants.CENTER), BorderLayout.NORTH);
        painel.add(new JScrollPane(lista), BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));

        JButton btnAdicionar = new JButton("Adicionar Música");
        btnAdicionar.addActionListener(e -> {
            String[] options = {"Adicionar Arquivo de Áudio (.wav)", "Colar Link do YouTube"};
            int escolha = JOptionPane.showOptionDialog(painel, "Escolha a forma de adicionar música", "Adicionar Música",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            if (escolha == 0) {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Selecione um arquivo .wav");
                fc.setFileFilter(new FileNameExtensionFilter("WAV", "wav"));
                if (fc.showOpenDialog(painel) == JFileChooser.APPROVE_OPTION) {
                    File arquivo = fc.getSelectedFile();
                    if (!arquivo.getName().toLowerCase().endsWith(".wav")) {
                        JOptionPane.showMessageDialog(painel, "Somente arquivos .wav");
                        return;
                    }
                    try {
                        File pasta = new File("audios");
                        if (!pasta.exists()) pasta.mkdir();
                        File destino = new File(pasta, arquivo.getName());
                        Files.copy(arquivo.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        Musica m = new Musica(arquivo.getName(), destino.getPath());
                        playlist.adicionar(m);
                        modeloPlaylist.addElement(m);
                        salvarPlaylist(playlist, ARQUIVO_PLAYLIST);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(painel, "Erro ao copiar arquivo.");
                    }
                }
            } else if (escolha == 1) {
                String url = JOptionPane.showInputDialog(painel, "Cole o link do YouTube:");
                if (url != null && !url.isEmpty()) {
                    try {
                        File pasta = new File("audios");
                        if (!pasta.exists()) pasta.mkdir();

                        // Nome de arquivo temporário sem extensão .mp3
                        String nomeBase = String.valueOf(System.currentTimeMillis());
                        String caminhoCompleto = "audios/" + nomeBase;

                        YouTubeDownloader.downloadAudioFromYouTube(url, caminhoCompleto);  // Salva como .wav

                        File arquivoBaixado = new File(caminhoCompleto + ".wav");
                        if (arquivoBaixado.exists()) {
                            Musica musica = new Musica(nomeBase + ".wav", arquivoBaixado.getPath());
                            playlist.adicionar(musica);
                            modeloPlaylist.addElement(musica);
                            salvarPlaylist(playlist, ARQUIVO_PLAYLIST);
                            JOptionPane.showMessageDialog(painel, "Música adicionada com sucesso!");
                        } else {
                            JOptionPane.showMessageDialog(painel, "Arquivo não encontrado após download.");
                        }

                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(painel, "Erro ao baixar áudio: " + ex.getMessage());
                    }
                }
            }
        });

        JButton btnRemover = new JButton("Remover");
        btnRemover.addActionListener(e -> {
            int idx = lista.getSelectedIndex();
            if (idx != -1) {
                playlist.remover(idx);
                modeloPlaylist.remove(idx);
                salvarPlaylist(playlist, ARQUIVO_PLAYLIST);
            } else JOptionPane.showMessageDialog(painel, "Selecione uma música");
        });

        JButton btnEditar = new JButton("Editar");
        btnEditar.addActionListener(e -> {
            int idx = lista.getSelectedIndex();
            if (idx != -1) {
                Musica m = modeloPlaylist.get(idx);
                String novoNome = JOptionPane.showInputDialog(painel, "Nome:", m.nome);
                String novoArtista = JOptionPane.showInputDialog(painel, "Artista:", m.artista);
                if (novoNome != null && novoArtista != null) {
                    if (!novoNome.matches("[a-zA-Z0-9 _-]+") || !novoArtista.matches("[a-zA-Z0-9 _-]+")) {
                        JOptionPane.showMessageDialog(painel, "Use apenas letras, números e espaços.");
                        return;
                    }
                    m.nome = novoNome.trim();
                    m.artista = novoArtista.trim();
                    lista.repaint();
                    salvarPlaylist(playlist, ARQUIVO_PLAYLIST);
                    atualizarFavoritos(favoritos, m);
                }
            } else {
                JOptionPane.showMessageDialog(painel, "Selecione uma música");
            }
        });

        JButton btnFavoritar = new JButton("Favoritar");
        btnFavoritar.addActionListener(e -> {
            int idx = lista.getSelectedIndex();
            if (idx != -1) {
                Musica m = modeloPlaylist.get(idx);
                if (!isMusicaNosFavoritos(favoritos, m)) {
                    favoritos.adicionar(m);
                    modeloFavoritos.addElement(m);
                    salvarPlaylist(favoritos, ARQUIVO_FAVORITOS);
                    JOptionPane.showMessageDialog(painel, "Adicionado aos favoritos");
                } else {
                    JOptionPane.showMessageDialog(painel, "Já está nos favoritos");
                }
            } else {
                JOptionPane.showMessageDialog(painel, "Selecione uma música");
            }
        });

        JButton btnReproduzir = new JButton("Reproduzir");
        btnReproduzir.addActionListener(e -> {
            int idx = lista.getSelectedIndex();
            if (idx != -1) tocarMusica(modeloPlaylist.get(idx).caminho, painel);
            else JOptionPane.showMessageDialog(painel, "Selecione uma música");
        });

        JButton btnPausar = new JButton("Pausar");
        btnPausar.addActionListener(e -> {
            if (clipAtual != null && clipAtual.isRunning()) clipAtual.stop();
        });

        botoes.add(btnAdicionar);
        botoes.add(btnRemover);
        botoes.add(btnEditar);
        botoes.add(btnFavoritar);
        botoes.add(btnReproduzir);
        botoes.add(btnPausar);

        painel.add(botoes, BorderLayout.SOUTH);
        return painel;
    }

    private static JPanel criarPainelFavoritos(Playlist favoritos, DefaultListModel<Musica> modeloFavoritos) {
        JList<Musica> lista = new JList<>(modeloFavoritos);
        lista.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lista.setFixedCellHeight(28);

        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        painel.add(new JLabel("Favoritos", SwingConstants.CENTER), BorderLayout.NORTH);
        painel.add(new JScrollPane(lista), BorderLayout.CENTER);

        JButton btnReproduzir = new JButton("Reproduzir");
        btnReproduzir.addActionListener(e -> {
            int idx = lista.getSelectedIndex();
            if (idx != -1) tocarMusica(modeloFavoritos.get(idx).caminho, painel);
            else JOptionPane.showMessageDialog(painel, "Selecione uma música");
        });

        JButton btnRemover = new JButton("Remover");
        btnRemover.addActionListener(e -> {
            int idx = lista.getSelectedIndex();
            if (idx != -1) {
                modeloFavoritos.remove(idx);
                favoritos.remover(idx);
                salvarPlaylist(favoritos, ARQUIVO_FAVORITOS);
            } else {
                JOptionPane.showMessageDialog(painel, "Selecione uma música");
            }
        });

        JButton btnPausar = new JButton("Pausar");
        btnPausar.addActionListener(e -> {
            if (clipAtual != null && clipAtual.isRunning()) clipAtual.stop();
        });

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        botoes.add(btnReproduzir);
        botoes.add(btnRemover);
        botoes.add(btnPausar);

        painel.add(botoes, BorderLayout.SOUTH);
        return painel;
    }

    private static boolean isMusicaNosFavoritos(Playlist favoritos, Musica musica) {
        for (Musica m : favoritos.musicas) {
            if (m.id.equals(musica.id)) {
                return true;
            }
        }
        return false;
    }

    private static void atualizarFavoritos(Playlist favoritos, Musica musicaAtualizada) {
        for (int i = 0; i < favoritos.musicas.size(); i++) {
            Musica m = favoritos.musicas.get(i);
            if (m.id.equals(musicaAtualizada.id)) {
                favoritos.musicas.set(i, musicaAtualizada);
                break;
            }
        }
        salvarPlaylist(favoritos, ARQUIVO_FAVORITOS);
    }

    private static void salvarPlaylist(Playlist playlist, File arquivo) {
        try (PrintWriter pw = new PrintWriter(arquivo)) {
            for (Musica m : playlist.musicas) {
                pw.println(m.id + "|" + m.nome + "|" + m.artista + "|" + m.caminho);
            }
        } catch (IOException e) {
            System.err.println("Erro ao salvar playlist: " + e.getMessage());
        }
    }

    private static void carregarPlaylist(Playlist playlist, File arquivo) {
        if (!arquivo.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split("\\|");
                if (partes.length >= 4) {
                    Musica m = new Musica(partes[1], partes[3]);
                    m.artista = partes[2];
                    m.id = partes[0];
                    playlist.adicionar(m);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar playlist: " + e.getMessage());
        }
    }

    private static void tocarMusica(String caminho, Component parent) {
        if (clipAtual != null && clipAtual.isRunning()) {
            clipAtual.stop();
            clipAtual.close();
        }

        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(caminho));
            clipAtual = AudioSystem.getClip();
            clipAtual.open(audioStream);
            clipAtual.start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Erro ao reproduzir: " + caminho);
        }
    }

    private static void aplicarTema(Component comp, boolean escuro) {
        Color fundoEscuro = new Color(34, 34, 34);
        Color fundoClaro = new Color(240, 240, 240);
        Color textoEscuro = Color.BLACK;
        Color textoClaro = Color.WHITE;

        if (escuro) {
            if (comp instanceof JPanel || comp instanceof JScrollPane || comp instanceof JTabbedPane || comp instanceof JFrame) {
                comp.setBackground(fundoEscuro);
            }
            if (comp instanceof JLabel || comp instanceof JButton || comp instanceof JList) {
                comp.setForeground(textoClaro);
                if (comp instanceof JList || comp instanceof JButton) {
                    comp.setBackground(fundoEscuro);
                    if (comp instanceof JButton) {
                        ((JButton) comp).setContentAreaFilled(true);
                        ((JButton) comp).setOpaque(true);
                    }
                }
            }
        } else {
            if (comp instanceof JPanel || comp instanceof JScrollPane || comp instanceof JTabbedPane || comp instanceof JFrame) {
                comp.setBackground(fundoClaro);
            }
            if (comp instanceof JLabel || comp instanceof JButton || comp instanceof JList) {
                comp.setForeground(textoEscuro);
                if (comp instanceof JList || comp instanceof JButton) {
                    comp.setBackground(fundoClaro);
                    if (comp instanceof JButton) {
                        ((JButton) comp).setContentAreaFilled(true);
                        ((JButton) comp).setOpaque(true);
                    }
                }
            }
        }

        if (comp instanceof Container) {
            for (Component filho : ((Container) comp).getComponents()) {
                aplicarTema(filho, escuro);
            }
        }
    }

    // ---------- CLASSES INTERNAS ----------

    static class Musica {
        String id;
        String nome;
        String artista = "";
        String caminho;
        String duracao = "";

        Musica(String nome, String caminho) {
            this.id = gerarId(caminho);
            this.nome = nome;
            this.caminho = caminho;
            this.duracao = calcularDuracao(caminho);
        }

        private String gerarId(String caminho) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(Files.readAllBytes(Paths.get(caminho)));
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (Exception e) {
                return "";
            }
        }

        private String calcularDuracao(String caminho) {
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(new File(caminho))) {
                AudioFormat format = stream.getFormat();
                long frames = stream.getFrameLength();
                float frameRate = format.getFrameRate();
                int totalSeconds = (int) (frames / frameRate);

                int minutos = totalSeconds / 60;
                int segundos = totalSeconds % 60;

                return String.format("%02d:%02d", minutos, segundos);
            } catch (Exception e) {
                return "00:00";
            }
        }

        @Override
        public String toString() {
            String art = artista.isEmpty() ? "" : " - " + artista;
            return nome + art + " [" + duracao + "]";
        }
    }

    static class Playlist {
        String nome;
        List<Musica> musicas = new ArrayList<>();

        Playlist(String nome) {
            this.nome = nome;
        }

        void adicionar(Musica m) {
            musicas.add(m);
        }

        void remover(int idx) {
            if (idx >= 0 && idx < musicas.size()) {
                musicas.remove(idx);
            }
        }
    }
}
