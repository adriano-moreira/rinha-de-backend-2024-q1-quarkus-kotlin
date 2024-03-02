import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType


@Path("/")
class ValidateResource(
    val clienteRepository: ClienteRepository,
    val transacaoRepository: TransacaoRepository,
) {

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun validate():String {
        val allClients = clienteRepository.listAll().sortedBy { it.id }

        var html = "<pre>"

        for (cliente in allClients) {
            html += "cliente: ${cliente.id}-${cliente.nome}  \n"
            html += "limite ${cliente.limite} \n"

            val transacoes = transacaoRepository.findByCliente(cliente.id!!)
            var saldo = 0L
            for (transacao in transacoes) {
                if(transacao.tipo == 'd') {
                    saldo -= transacao.valor!!
                }
                if(transacao.tipo == 'c') {
                    saldo += transacao.valor!!
                }
            }

            html += "saldo: ${cliente.saldo} \n"
            html += "saldo calculado: $saldo \n"
            html += "qtd transacoes: ${transacoes.size} \n"
            if(saldo != cliente.saldo) {
                html += "<<<<<<< Não bateu o saldo  >>>>> \n"
            }

            //...
            html += "--------------------------------\n"
        }
        html += "</pre>"
        return html
    }

}
