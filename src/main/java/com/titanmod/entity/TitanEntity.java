package com.titanmod.entity;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * O Titã. Regra do multiplayer: o SERVIDOR decide vida, escudo, dono e cargas de dash.
 * O cliente só recebe os valores sincronizados via SynchedEntityData e renderiza.
 *
 * M2: o dono pode embarcar (clique direito), pilotar com WASD + mouse e usar o dash.
 * Como em qualquer veículo do Minecraft (cavalo, barco), o MOVIMENTO é calculado no cliente do
 * piloto e o servidor aceita a posição; o servidor controla apenas os recursos (cargas de dash).
 */
public class TitanEntity extends PathfinderMob {

    // ---- Valores de balanceamento (depois podem ir para config / TitanClass) ----
    public static final int MAX_SHIELD = 400;
    /** Ticks sem dano antes do escudo começar a regenerar (100 ticks = 5 s). */
    public static final int SHIELD_REGEN_DELAY = 100;
    /** Escudo regenerado por tick (4/tick = 80/s, ~5 s para encher). */
    public static final int SHIELD_REGEN_PER_TICK = 4;

    public static final int MAX_DASH_CHARGES = 2;
    /** Ticks para recuperar UMA carga de dash (60 ticks = 3 s). */
    public static final int DASH_RECHARGE_TICKS = 60;
    /** Força do impulso horizontal do dash (aplicado no cliente do piloto). */
    public static final double DASH_STRENGTH = 2.0D;

    // ---- Dados sincronizados servidor -> clientes ----
    private static final EntityDataAccessor<Integer> DATA_SHIELD =
            SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AI_MODE =
            SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_DASH_CHARGES =
            SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);

    /** Só existe no servidor: tick (gameTime) do último dano recebido. */
    private long lastDamageTime = Long.MIN_VALUE / 2;
    /** Só existe no servidor: progresso da recarga da próxima carga de dash. */
    private int dashRechargeTicks = 0;

    public TitanEntity(EntityType<? extends TitanEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(); // nunca some sozinho
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.STEP_HEIGHT, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 30.0D);
    }

    @Override
    protected void registerGoals() {
        // Só flutuar na água. Os modos Seguir/Guardar entram no M5.
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHIELD, MAX_SHIELD);
        builder.define(DATA_AI_MODE, TitanAIMode.FOLLOW.ordinal());
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_DASH_CHARGES, MAX_DASH_CHARGES);
    }

    // ------------------------------------------------------------------ getters / setters

    public int getShield() {
        return this.entityData.get(DATA_SHIELD);
    }

    public void setShield(int value) {
        this.entityData.set(DATA_SHIELD, Math.max(0, Math.min(MAX_SHIELD, value)));
    }

    public int getDashCharges() {
        return this.entityData.get(DATA_DASH_CHARGES);
    }

    private void setDashCharges(int value) {
        this.entityData.set(DATA_DASH_CHARGES, Math.max(0, Math.min(MAX_DASH_CHARGES, value)));
    }

    public TitanAIMode getAIMode() {
        return TitanAIMode.byOrdinal(this.entityData.get(DATA_AI_MODE));
    }

    public void setAIMode(TitanAIMode mode) {
        this.entityData.set(DATA_AI_MODE, mode.ordinal());
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER, Optional.ofNullable(uuid));
    }

    public boolean isOwnedBy(Player player) {
        UUID owner = getOwnerUUID();
        return owner != null && owner.equals(player.getUUID());
    }

    // ------------------------------------------------------------------ piloto / montaria

    /** Só um piloto por Titã. */
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    /** O jogador que está dentro é quem controla o Titã. */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof Player player ? player : null;
    }

    /**
     * Movimento controlado pelo piloto: o Titã gira junto com a mira do jogador e anda
     * conforme as teclas WASD. Roda no cliente do piloto (quem controla o veículo).
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.isAlive() && this.getControllingPassenger() instanceof Player rider) {
            this.setYRot(rider.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.5F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.yBodyRot;

            float strafe = rider.xxa * 0.6F;
            float forward = rider.zza;
            if (forward <= 0.0F) {
                forward *= 0.4F; // andar de ré é mais lento
            }
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
            super.travel(new Vec3(strafe, travelVector.y, forward));
        } else {
            super.travel(travelVector);
        }
    }

    // ------------------------------------------------------------------ dash

    /**
     * Chamado pelo servidor quando o piloto pede um dash. Gasta uma carga, se houver.
     * O impulso em si é aplicado no cliente do piloto (veículo controlado pelo cliente).
     */
    public boolean tryConsumeDash() {
        if (this.level().isClientSide || getDashCharges() <= 0) {
            return false;
        }
        setDashCharges(getDashCharges() - 1);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.2F, 1.4F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3D, this.getZ(),
                    16, 0.8D, 0.1D, 0.8D, 0.03D);
        }
        return true;
    }

    private void tickDashRecharge() {
        if (getDashCharges() >= MAX_DASH_CHARGES) {
            dashRechargeTicks = 0;
            return;
        }
        if (++dashRechargeTicks >= DASH_RECHARGE_TICKS) {
            dashRechargeTicks = 0;
            setDashCharges(getDashCharges() + 1);
        }
    }

    // ------------------------------------------------------------------ lógica de servidor

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.isAlive()) {
            tickShieldRegen();
            tickDashRecharge();
        }
    }

    private void tickShieldRegen() {
        if (getShield() >= MAX_SHIELD) {
            return;
        }
        long now = this.level().getGameTime();
        if (now - lastDamageTime >= SHIELD_REGEN_DELAY) {
            setShield(getShield() + SHIELD_REGEN_PER_TICK);
        }
    }

    /**
     * O dano passa primeiro pelo escudo; só o que sobrar chega na vida.
     * Dano que ignora invulnerabilidade (void, /kill) atravessa o escudo.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || !this.isAlive()) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }

        this.lastDamageTime = this.level().getGameTime();

        int shield = getShield();
        if (shield > 0) {
            float absorbed = Math.min(shield, amount);
            setShield(shield - (int) Math.ceil(absorbed));
            float remaining = amount - absorbed;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    shield - absorbed <= 0 ? SoundEvents.SHIELD_BREAK : SoundEvents.ANVIL_PLACE,
                    SoundSource.HOSTILE, 0.6F, 1.4F);
            if (remaining <= 0.0F) {
                return true; // tudo absorvido pelo escudo
            }
            amount = remaining;
        }
        return super.hurt(source, amount);
    }

    /** Mecha pesado não sofre dano de queda. */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /**
     * Clique direito:
     *  - Titã sem dono: quem clicar vira o dono.
     *  - Dono: embarca. Agachado, mostra o status (vida/escudo) em vez de embarcar.
     *  - Outro jogador: recebe aviso de que o Titã não é dele.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (getOwnerUUID() == null) {
            setOwnerUUID(player.getUUID());
        }
        if (!isOwnedBy(player)) {
            player.displayClientMessage(Component.translatable("message.titanmod.not_owner"), true);
            return InteractionResult.CONSUME;
        }
        if (player.isSecondaryUseActive()) {
            player.displayClientMessage(Component.translatable("message.titanmod.status",
                    (int) this.getHealth(), (int) this.getMaxHealth(), getShield(), MAX_SHIELD), true);
            return InteractionResult.CONSUME;
        }
        if (!this.isVehicle()) {
            player.startRiding(this);
        }
        return InteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------ persistência (NBT)

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Shield", getShield());
        tag.putInt("DashCharges", getDashCharges());
        tag.putInt("AIMode", this.entityData.get(DATA_AI_MODE));
        UUID owner = getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Shield")) {
            setShield(tag.getInt("Shield"));
        }
        if (tag.contains("DashCharges")) {
            setDashCharges(tag.getInt("DashCharges"));
        }
        if (tag.contains("AIMode")) {
            this.entityData.set(DATA_AI_MODE, tag.getInt("AIMode"));
        }
        if (tag.hasUUID("Owner")) {
            setOwnerUUID(tag.getUUID("Owner"));
        }
    }
}
