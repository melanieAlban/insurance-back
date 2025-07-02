Feature: Obtener usuarios
  Como administrador
  Quiero obtener la lista de todos los usuarios
  Para visualizar la información registrada

  Scenario: Obtener todos los usuarios existentes
    Given que estoy autenticado como administrador
    And existen usuarios registrados en el sistema
    When solicito la lista de usuarios
    Then debo recibir una lista con al menos 1 usuario
