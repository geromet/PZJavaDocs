/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import java.util.TreeMap;
import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.RoomDef;
import zombie.iso.zones.RoomTone;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public final class ParameterRoomTypeEx
extends FMODGlobalParameter {
    private static final IsoDirections[] DIRECTIONS = IsoDirections.values();
    static ParameterRoomTypeEx instance;
    static RoomType roomType;

    public ParameterRoomTypeEx() {
        super("RoomTypeEx");
        instance = this;
    }

    @Override
    public float calculateCurrentValue() {
        return this.getRoomType().label;
    }

    private RoomType getRoomType() {
        if (roomType != null) {
            return roomType;
        }
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        if (character == null) {
            return RoomType.Generic;
        }
        BuildingDef buildingDef = character.getCurrentBuildingDef();
        if (buildingDef == null) {
            return RoomType.Generic;
        }
        RoomDef currentRoomDef = character.getCurrentRoomDef();
        int cellX = PZMath.fastfloor(character.getX() / 256.0f);
        int cellY = PZMath.fastfloor(character.getY() / 256.0f);
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                RoomType roomType1 = this.getRoomType(currentRoomDef, cellX + dx, cellY + dy);
                if (roomType1 == null) continue;
                return roomType1;
            }
        }
        return RoomType.Generic;
    }

    private RoomType getRoomType(RoomDef currentRoomDef, int cellX, int cellY) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
        if (metaCell == null || metaCell.roomTones.isEmpty()) {
            return null;
        }
        RoomTone roomToneForBuilding = null;
        for (int i = 0; i < metaCell.roomTones.size(); ++i) {
            RoomTone roomTone = metaCell.roomTones.get(i);
            RoomDef roomDef = metaGrid.getRoomAt(roomTone.x, roomTone.y, roomTone.z);
            if (roomDef == null) continue;
            if (roomDef == currentRoomDef) {
                return RoomType.lowercaseMap.getOrDefault(roomTone.enumValue, RoomType.Generic);
            }
            if (!roomTone.entireBuilding || roomDef.building != currentRoomDef.getBuilding() || roomToneForBuilding != null && (currentRoomDef.level >= roomToneForBuilding.z || currentRoomDef.level < roomTone.z)) continue;
            roomToneForBuilding = roomTone;
        }
        if (roomToneForBuilding != null) {
            return RoomType.lowercaseMap.getOrDefault(roomToneForBuilding.enumValue, RoomType.Generic);
        }
        return null;
    }

    public static void setRoomType(int roomType) {
        try {
            ParameterRoomTypeEx.roomType = RoomType.values()[roomType];
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            ParameterRoomTypeEx.roomType = null;
        }
    }

    public static void render(IsoPlayer player) {
        if (instance == null) {
            return;
        }
        if (player != FMODParameterUtils.getFirstListener()) {
            return;
        }
        if (player != IsoCamera.frameState.camCharacter) {
            return;
        }
        RoomDef roomDef = player.getCurrentRoomDef();
        player.drawDebugTextBelow("RoomDef.name : " + (roomDef == null ? "null" : roomDef.name) + "\nRoomTypeEx : " + instance.getRoomType().name());
    }

    public static void renderRoomTones() {
        int cellX = PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 256.0f);
        int cellY = PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 256.0f);
        ParameterRoomTypeEx.renderRoomTones(cellX, cellY);
        for (int i = 0; i < DIRECTIONS.length; ++i) {
            IsoDirections dir = DIRECTIONS[i];
            ParameterRoomTypeEx.renderRoomTones(cellX + dir.dx(), cellY + dir.dy());
        }
    }

    private static void renderRoomTones(int cellX, int cellY) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
        if (metaCell == null || metaCell.roomTones.isEmpty()) {
            return;
        }
        PlayerCamera camera = IsoCamera.cameras[IsoCamera.frameState.playerIndex];
        for (int i = 0; i < metaCell.roomTones.size(); ++i) {
            RoomTone roomTone = metaCell.roomTones.get(i);
            float fontHeight = TextManager.instance.getFontHeight(UIFont.Small);
            int boxWidth = TextManager.instance.MeasureStringX(UIFont.Small, roomTone.enumValue);
            int boxHeight = (int)fontHeight;
            float sx = IsoUtils.XToScreen((float)roomTone.x + 0.5f + camera.fixJigglyModelsSquareX * 0.0f, (float)roomTone.y + 0.5f + camera.fixJigglyModelsSquareY * 0.0f, roomTone.z, 0);
            float sy = IsoUtils.YToScreen((float)roomTone.x + 0.5f + camera.fixJigglyModelsSquareX * 0.0f, (float)roomTone.y + 0.5f + camera.fixJigglyModelsSquareY * 0.0f, roomTone.z, 0);
            sy -= (float)boxHeight / 2.0f;
            sx -= camera.getOffX();
            sy -= camera.getOffY();
            SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
            SpriteRenderer.instance.renderi(null, (int)((sx /= camera.zoom) - (float)(boxWidth / 2)), (int)((sy /= camera.zoom) - ((float)boxHeight - fontHeight) / 2.0f), boxWidth, boxHeight, 0.0f, 0.0f, 0.0f, 0.5f, null);
            TextManager.instance.DrawStringCentre(UIFont.Small, sx, sy, roomTone.enumValue, 1.0, 1.0, 1.0, 1.0);
            SpriteRenderer.instance.EndShader();
        }
    }

    private static enum RoomType {
        Generic(0),
        AbandonedGrainmill(1),
        Airport(2),
        AirstripOffice(3),
        AnimalsanctuaryKennels(4),
        AnimalsanctuaryMain(5),
        AnimalShelter(6),
        ApartmentBuilding(7),
        Arena01Derby(8),
        Arena02Stables(9),
        Arena04Publicbathroom(10),
        Arena07Offices(11),
        Arena08Changerooms(12),
        Arena10Broadcasting(13),
        Arena12Baseball(14),
        Arena13Indoors(15),
        Bakery(16),
        Bank(17),
        BarWood(18),
        BarStone(19),
        Bargnclothes(20),
        Barn(21),
        BasementApartmnetBuilding(22),
        BasementBank(23),
        BasementBar(24),
        BasementDestroyedBuilding(25),
        BasementChurch(26),
        BasementColdwarBunker(27),
        BasementConstruction01Factory(28),
        BasementDetentionCentre(29),
        BasementFactoryAbandoned(30),
        BasementFirestation(31),
        BasementGas(32),
        BasementGeneralstore(33),
        BasementGovernmentSecret(34),
        BasementGunclub(35),
        BasementHouse(36),
        BasementHouseEastereggSpiffo(37),
        BasementIndustrialMedium(38),
        BasementOffices(39),
        BasementPaddlewheeler(40),
        BasementPlaza(41),
        BasementPolice(42),
        BasementRestaurant(43),
        BasementSchool(44),
        BasementStorefront(45),
        Bedandbreakfast(46),
        Boatclub(47),
        Boathouse(48),
        Bookstore(49),
        Bowlingalley(50),
        Brewery(51),
        BrownbrickTownhouse(52),
        BuildingDestroyed(53),
        Busshelter(54),
        Cabin(55),
        CabinAbandoned(56),
        Cardealership(57),
        Carsupplystore(58),
        Cattleauction(59),
        Cattlelot(60),
        Changerooms(61),
        Church(62),
        ChurchAbandoned(63),
        ChurchBurnt(64),
        CivilianMedia(65),
        CivilianTent(66),
        Cleaningservices(67),
        Clothesstore(68),
        Coffeeshop(69),
        CaldwarBunker(70),
        CommunityCentre(71),
        ConvenienceStoreBurnt(72),
        ConventionHall(73),
        Cornerstore(74),
        Countryclub(75),
        CountryclubGolfcarts(76),
        CountryclubMaintenance(77),
        CountryclubMechanic(78),
        CountryclubClubhouse(79),
        CountryclubChangerooms(80),
        CountryclubSauna(81),
        CountryclubSnackbar(82),
        CountryclubGuardbooth(83),
        Departmentstore(84),
        Depository(85),
        Detentioncenter(86),
        Diner(87),
        Distillery(88),
        Dock(89),
        Emptystore(90),
        Factory(91),
        FarmStorage(92),
        Farmhouse(93),
        Farmhousing(94),
        Farmstore(95),
        Firestation(96),
        Fishbait(97),
        Fitnesscenter(98),
        Freight(99),
        Funeralhome(100),
        Furniturestore(101),
        Gallery(102),
        GarageBurnt(103),
        Garage(104),
        Garagestorage(105),
        Gardenstore(106),
        Gas(107),
        GasBurnt(108),
        Gatehouse(109),
        Generalstore(110),
        Government(111),
        Greenhouse(112),
        Grocery(113),
        Groundskeeper(114),
        Guardbooth(115),
        Gunclub(116),
        Gym(117),
        Hairdresser(118),
        Hanger(119),
        Hayshipping(120),
        HighriseMgCenter(121),
        HorseridingCentre(122),
        Hospital(123),
        Hotel(124),
        HouseAbandoned(125),
        HouseBurnt(126),
        HouseCountry(127),
        HouseDestroyed(128),
        HouseGarage(129),
        HouseGarageDamaged(130),
        HouseLake(131),
        HouseLarge(132),
        HouseMedium(133),
        HouseMediumDamaged(134),
        HouseSmall(135),
        HouseSmallDamaged(136),
        HouseSuburb(137),
        HouseSuburbGarage(138),
        HouseTrapper(139),
        IndustrialWarehouse(140),
        IndustryDorm(141),
        Kicthenshowroom(142),
        Lakehouse(143),
        Lasertag(144),
        Laundromat(145),
        LaundromatBurnt(146),
        Library(147),
        LibraryBurnt(148),
        Liquorstore(149),
        Lofts(150),
        Mall(151),
        Mansion(152),
        Mausoleum(153),
        Mechanic(154),
        Medical(155),
        MilitaryApartments(156),
        MilitaryBorder(157),
        MilitaryCommunications(158),
        MilitaryHouse(159),
        MilitaryTent(160),
        MilitaryTower(161),
        MilitaryTownhouse(162),
        MilitaryTrailer(163),
        Motel(164),
        MotelDestroyed(165),
        Movierental(166),
        MusicfestivalTower(167),
        MusicfestivalTrailer(168),
        Musicstore(169),
        Nursinghome(170),
        Offices(171),
        Officesupplies(172),
        Optometrist(173),
        Outhouse(174),
        Paddlewheeler(175),
        Paintshop(176),
        ParkTreehouse(177),
        ParkRestrooms(178),
        ParkPlayhouse(179),
        Pawnshop(180),
        Paybooth(181),
        Pharmacy(182),
        Photobooth(183),
        Pizzawhirled(184),
        PizaawhirledBurnt(185),
        Plaza(186),
        PlazaBurnt(187),
        Police(188),
        PoliceBurnt(189),
        Portacabin(190),
        Post(191),
        Prison(192),
        PrisonGuardbooth(193),
        PrisonGuardtower(194),
        Publicwashroom(195),
        RadioTowerHouse(196),
        RadioTowerStation(197),
        Radiostation(198),
        Rangerstation(199),
        Reccenter(200),
        Recordingstudio(201),
        RedbrickTownhouse(202),
        Refinery(203),
        Resort(204),
        ResortCabin(205),
        RestareaBathrooms(206),
        Restaurant(207),
        Rollerrink(208),
        Sanatorium(209),
        School(210),
        SchoolColege(211),
        SchoolDorms(212),
        ScrapyardGarage(213),
        Security(214),
        Shack(215),
        Shed(216),
        Shelter(217),
        Shootingrange(218),
        ShopCamping(219),
        ShopHunting(220),
        Singleapartment(221),
        Slaughterhouse(222),
        Spa(223),
        Speedway(224),
        Sportstore(225),
        Stables(226),
        StoreAbandoned(227),
        Storefront(228),
        Summercamp(229),
        Taxi(230),
        TaxiGarage(231),
        Theatre(232),
        Thermalpowerstation(233),
        Tobaccostore(234),
        Tollbooth(235),
        Toolstore(236),
        Tornadoshelter(237),
        Townhouse(238),
        Trailer(239),
        TrailerBurnt(240),
        TrailerNice(241),
        TrailerOffice(242),
        Traincar(243),
        Trainstation(244),
        TrainstationAbadoned(245),
        Trainyard(246),
        TrainyardOffice(247),
        Transportcompany(248),
        Treehouse(249),
        Truck(250),
        Tvstudio(251),
        Vetrenarian(252),
        WadsworthBasemenet(253),
        Warehousestorage(254),
        WaterpurificationCheckpoint(255),
        WaterpurificationMainBuilding(256),
        WaterpurificationOffice(257),
        Weddingstore(258),
        Weldingshop(259),
        Westpointcaboose(260),
        Wildwestoffice(261),
        WoodCabin(262),
        WreckingyardOffice(263),
        Zippee(264),
        Victorian(265);

        static final TreeMap<String, RoomType> lowercaseMap;
        final int label;

        private RoomType(int label) {
            this.label = label;
        }

        static {
            lowercaseMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
            for (RoomType e : RoomType.values()) {
                lowercaseMap.put(e.name(), e);
            }
        }
    }
}

