package com.devmh.controller;

import com.devmh.model.ApprovalState;
import com.devmh.model.Case;
import com.devmh.model.Docket;
import com.devmh.model.Location;
import com.devmh.persistence.CaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/cases")
class CaseController {

    private final CaseRepository caseRepository;
    private final Random rand  = new Random();
    {
        rand.setSeed(System.currentTimeMillis());
    }

    public CaseController(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @PostMapping
    public Case createCase(@RequestBody Case newCase) {
        return caseRepository.save(newCase);
    }

    @GetMapping
    public Page<Case> getAllCases() {
        return (Page<Case>)caseRepository.findAll();
    }

    @GetMapping("/new")
    public Case newCase() {
        Case newCase = generateCase();
        return caseRepository.save(newCase);
    }

    @GetMapping("/bulk")
    public long bulkNewCases() {
        Instant start = Instant.now();
        for(int i = 0; i < 10000; i++) {
            Case newCase = generateCase();
            caseRepository.save(newCase);
        }
        return Instant.now().getNano() - start.getNano();
    }

    private Case generateCase() {
        return Case.builder()
                .id(UUID.randomUUID())
                .name(randomName("Case"))
                .docket(generateDocket())
                .location(generateLocation())
                .state(generateApprovalState())
                .build();
    }

    private Location generateLocation() {
        return Location.values()[rand.nextInt(Location.values().length)];
    }

    private ApprovalState generateApprovalState() {
        return ApprovalState.values()[rand.nextInt(ApprovalState.values().length)];
    }

    private Docket generateDocket() {
        return Docket.builder()
                .id(UUID.randomUUID())
                .name(randomName("Docket"))
                .build();
    }

    private String randomName(String typeName) {
        int index = rand.nextInt(COLOR.size());
        String color = COLOR.get(index);
        index = rand.nextInt(ANIMAL.size());
        String animal = ANIMAL.get(index);
        index = rand.nextInt(100000);
        return typeName + color + animal + index;
    }

    private static final List<String> COLOR = List.of(
            "Red","Blue","Green","Yellow","Orange","Pink","Purple","Brown",
            "Black","White","Gray","Silver","Gold","Tan","Beige","Coral","Indigo",
            "Violet","Crimson","Teal","Navy","Charcoal","Mint","Lavender","Aqua",
            "Plum","Peach","Ruby","Amber","Lime","Cobalt","Turquoise","Mahogany",
            "Periwinkle","Fuchsia","Emerald","Sapphire","Topaz","Maroon","Azure",
            "Scarlet","Chartreuse","Onyx","Lemon","Rose","Ivory","Moss","Slate",
            "Burgundy","Cerulean","Champagne","Jade","Celeste","Copper","Tangerine",
            "Seafoam","Apricot","Wheat","Denim","Sand","Cocoa","Poppy","Honey",
            "Plum","Lemonade","Lilac","Snow","Sunflower","Vanilla","Pistachio",
            "Cotton","Grape","Pine","Canary","Zinc","Amethyst","Brass","Ochre"
    );

    private static final List<String> ANIMAL = List.of(
            "Lion","Tiger","Elephant","Giraffe","Zebra","Kangaroo","Koala","Panda",
            "Bear","Wolf","Fox","Squirrel","Deer","Bison","Leopard","Cheetah",
            "Hippopotamus","Rhinoceros","Gorilla","Chimpanzee","Orangutan","Baboon",
            "Lemur","Sloth","Otter","Beaver","Badger","Armadillo","Raccoon","Skunk",
            "Weasel","Mongoose","Meerkat","Hyena","Wild boar","Bat","Eagle","Hawk","Owl",
            "Vulture","Flamingo","Penguin","Ostrich","Parrot","Crow","Raven","Dove","Pigeon",
            "Peacock","Toucan","Swan","Crane","Pelican","Hummingbird","Woodpecker","Sparrow",
            "Robin","Seagull","Puffin","Shrimp","Lobster","Crab","Octopus",
            "Squid","Seahorse","Jellyfish","Starfish","Clam","Oyster","Mussel","Turtle",
            "Tortoise","Alligator","Crocodile","Lizard","Chameleon","Gecko","Frog","Toad",
            "Salamander","Snake","Komodo dragon","Monitor lizard","Camel","Horse","Donkey",
            "Mule","Cow","Sheep","Goat","Pig","Chicken","Duck","Turkey","Goose","Hamster",
            "Gerbil","Ferret","Rabbit","Chinchilla","Parakeet","Canary","Macaw","Finch",
            "Cockatoo","Hedgehog","Llama","Alpaca","Emu","Wombat","Antelope",
            "Gazelle","Springbok","Impala","Wildebeest","Yak","Muskox","Reindeer","Caribou"
    );
}
