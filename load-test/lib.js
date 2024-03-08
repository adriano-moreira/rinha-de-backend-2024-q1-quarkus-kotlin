// @ts-check

import { check, fail } from 'k6'
import http from 'k6/http'

const urlBase = 'http://localhost:9999'

/**
 * JsDoc
 * @param {Number|String} clienteId
 * @param {Object} payload
 * @param {Number} payload.valor
 * @param {String} payload.tipo
 * @param {String} payload.descricao
 */
export function postTransacao(clienteId, payload) {

//JSON XML
    const post = http.post(
        `${urlBase}/cliente/${clienteId}/transacoes`,
        JSON.stringify(payload),
        {
            responseType: 'text',
            headers: {
                'Content-Type': 'application/json',
            }
        },
    )

    if (post.status != 200) {
        console.error(`params: ${clienteId} ${JSON.stringify(payload, null, 4)}`)
        console.error(`http-status: ${post.status}`)
        console.error(`body: ${post.body}`)
        fail()
    }
    return post
}


/**
 *
 * @param {Number|String} clienteId
 */
export function getExtrato(clienteId) {
    return http.get(
        `${urlBase}/cliente/${clienteId}/extrato`,
        {
            responseType: 'text',
        },
    )
}


export default function () {
    const clienteId = 1
    const tipo = 'c'
    for (let i = 1; i <= 10; i++) {
        const post = postTransacao(
            clienteId,
            {
                valor: i,
                tipo: tipo,
                descricao: `desc ${i}`,
            },
        )
        check(post, { 'deve retornar 200': (post) => post.status == 200 })
    }

    const resp = getExtrato(clienteId);
    check(resp, { 'extrato deve retornar 200': (resp) => resp.status == 200 })

}
