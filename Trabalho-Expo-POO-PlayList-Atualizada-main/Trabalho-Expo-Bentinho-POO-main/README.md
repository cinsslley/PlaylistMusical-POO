# Trabalho-Expo-Bentinho-POO

Este é um pequeno projeto onde queriamos simular uma playlist de músicas e adoramos o resultado, esperam que curtam também, agradecimento do grupo Bentinho

btnEditar.addActionListener(e -> {
    int idx = lista.getSelectedIndex();
    if (idx != -1) {
        Musica m = modeloPlaylist.get(idx);
        String novoNome = JOptionPane.showInputDialog(painel, "Nome:", m.nome);
        String novoArtista = JOptionPane.showInputDialog(painel, "Artista:", m.artista);
        if (novoNome != null && novoArtista != null) {
            novoNome = novoNome.trim();
            novoArtista = novoArtista.trim();
            if (isTextoSomente(novoNome) && isTextoSomente(novoArtista)) {
                m.nome = novoNome;
                m.artista = novoArtista;
                lista.repaint();
                salvarPlaylist(playlist, ARQUIVO_PLAYLIST);
            } else {
                JOptionPane.showMessageDialog(painel, "Use somente letras e espaços para Nome e Artista.");
            }
        }
    } else {
        JOptionPane.showMessageDialog(painel, "Selecione uma música");
    }
});

// Método para validar texto somente letras e espaços
private static boolean isTextoSomente(String texto) {
    return texto != null && texto.matches("[a-zA-ZÀ-ÿ ]+");
}
