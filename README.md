## Comandi Master e Client
Comandi Client (da inviare al Master via socket)

- `REGISTER <IP> <PORT> <R1,R2,...>`
    - Registra un peer al master con una lista di risorse
    - Esempio: `REGISTER 127.0.0.1 5001 R1,R2`

- `GET <RISORSA>`
    - Richiede quale peer possiede la risorsa
    - Esempio: `GET R2`

- `REMOVE <IP> <PORT>`
    - Rimuove il peer dal sistema (es. in caso di disconnessione)
    - Esempio: `REMOVE 127.0.0.1 5001`

- `DOWNLOAD <RISORSA> <IP_DEST> <PORT_DEST>`
    - Registra il download di una risorsa da un peer verso un altro
    - Esempio: `DOWNLOAD R1 127.0.0.1 5002`

- `QUIT`
    - Chiude la connessione del client con il master

Comandi Console del Master

- `listdata`
    - Stampa tutte le risorse condivise e i peer che le

l'ho guardato ti direi di fare un po' di prove, magari utilizzando carichi un po' più impegnativi (i.e. magari usare più peer e con loop gli fai mandare un po di messaggi a piaggerella in contemporanea su uno, con un contatore per vedere se dei messaggi vengono persi/duplicati o robe simili). Alla fine mi viene da dire che o si mette nello stack il socket o si usa un id per le transazioni e si completano le cose in maniera disgiunta

@Main.java @LogEntry.java @Master.java @Peer.java @PeerListener.java @ResourceManager.java @Triplet.java @Tuple.java 