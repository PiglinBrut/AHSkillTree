package ru.pb.ahst.config;

public enum BlockedAction {
    ATTACK_ENTITY,              // ЛКМ — атака моба/игрока
    RIGHT_CLICK,                // ПКМ в воздухе
    USE_ON_BLOCK,               // ПКМ по блоку (использование предмета на блоке)
    PLACE_BLOCK,                // Размещение блока (установка стола зачарований, маяка и т.д.)
    BREAK_BLOCK,                // Разрушение блоков
    BREAK_BLOCK_BY_ITEM,        // Разрушение блоков с помощью предметов
    INTERACT_BLOCK,             // Взаимодействие с уже установленным блоком (открытие GUI)
    UNPREPAREDNESS_FOR_WEAPON,  // Штрафы при использовании
    EQUIP_ARMOR,                // Полная блокировка надевания
    UNPREPAREDNESS_FOR_ARMOR    // Штрафы при ношении
}