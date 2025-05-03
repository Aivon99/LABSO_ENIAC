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
