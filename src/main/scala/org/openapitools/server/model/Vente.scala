package org.openapitools.server.model


/**
 * @param consommation  for example: ''null''
 * @param quantite  for example: ''null''
 * @param produit  for example: ''null''
*/
final case class Vente (
  consommation: String,
  quantite: Int,
  produit: Int
)

