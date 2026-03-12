/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.physics;

import java.util.ArrayList;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.physics.Bullet;
import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.core.random.Rand;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.pathfind.VehiclePoly;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.EngineRPMData;
import zombie.vehicles.TransmissionNumber;

public final class CarController {
    public final BaseVehicle vehicleObject;
    public float clientForce;
    public float engineForce;
    public float brakingForce;
    private float vehicleSteering;
    private boolean isGas;
    private boolean isGasR;
    private boolean isBreak;
    private float atRestTimer = -1.0f;
    private float regulatorTimer;
    public boolean isEnable;
    private final Transform tempXfrm = new Transform();
    private final Vector2 tempVec2 = new Vector2();
    private final Vector3f tempVec3f = new Vector3f();
    private final Vector3f tempVec3f2 = new Vector3f();
    private final Vector3f tempVec3f3 = new Vector3f();
    private static final Vector3f _UNIT_Y = new Vector3f(0.0f, 1.0f, 0.0f);
    public boolean acceleratorOn;
    public boolean brakeOn;
    public float speed;
    public static GearInfo[] gears = new GearInfo[3];
    public final ClientControls clientControls = new ClientControls();
    private boolean engineStartingFromKeyboard;
    private static final BulletVariables bulletVariables;
    float drunkDelayCommandTimer;
    boolean wasBreaking;
    boolean wasGas;
    boolean wasGasR;
    boolean wasSteering;

    public CarController(BaseVehicle vehicleObject) {
        this.vehicleObject = vehicleObject;
        this.engineStartingFromKeyboard = false;
        VehicleScript script = vehicleObject.getScript();
        float physicsZ = vehicleObject.savedPhysicsZ;
        if (Float.isNaN(physicsZ)) {
            float wheelBottom = 0.0f;
            if (script.getWheelCount() > 0) {
                Vector3f modelOffset = script.getModelOffset();
                wheelBottom += modelOffset.y();
                wheelBottom += script.getWheel(0).getOffset().y() - script.getWheel((int)0).radius;
            }
            float chassisBottom = script.getCenterOfMassOffset().y() - script.getExtents().y() / 2.0f;
            physicsZ = (float)(PZMath.fastfloor(vehicleObject.getZ()) * 3) * 0.8164967f - Math.min(wheelBottom, chassisBottom);
            if (script.getWheelCount() == 0) {
                physicsZ = PZMath.max(physicsZ, (float)(PZMath.fastfloor(vehicleObject.getZ()) * 3) * 0.8164967f + 0.1f);
            }
            vehicleObject.jniTransform.origin.y = physicsZ;
        }
        if (!GameServer.server) {
            Bullet.addVehicle(vehicleObject.vehicleId, vehicleObject.getX(), vehicleObject.getY(), physicsZ, vehicleObject.savedRot.x, vehicleObject.savedRot.y, vehicleObject.savedRot.z, vehicleObject.savedRot.w, script.getFullName());
        }
    }

    public GearInfo findGear(float speed) {
        for (int i = 0; i < gears.length; ++i) {
            if (!(speed >= (float)CarController.gears[i].minSpeed) || !(speed < (float)CarController.gears[i].maxSpeed)) continue;
            return gears[i];
        }
        return null;
    }

    public void accelerator(boolean apply2) {
        this.acceleratorOn = apply2;
    }

    public void brake(boolean apply2) {
        this.brakeOn = apply2;
    }

    public ClientControls getClientControls() {
        return this.clientControls;
    }

    public void update() {
        if (this.vehicleObject.getVehicleTowedBy() != null) {
            return;
        }
        VehicleScript script = this.vehicleObject.getScript();
        this.speed = this.vehicleObject.getCurrentSpeedKmHour();
        boolean bDrunkDriver = this.vehicleObject.getDriver() != null && this.vehicleObject.getDriver().getMoodles().getMoodleLevel(MoodleType.DRUNK) > 1;
        float dot = 0.0f;
        Vector3f velocity = this.vehicleObject.getLinearVelocity(this.tempVec3f2);
        velocity.y = 0.0f;
        if ((double)velocity.length() > 0.5) {
            velocity.normalize();
            Vector3f forward = this.tempVec3f;
            this.vehicleObject.getForwardVector(forward);
            dot = velocity.dot(forward);
        }
        float limitMod = 1.0f;
        if (GameClient.client) {
            float delta = this.vehicleObject.getCurrentSpeedKmHour() / Math.min(120.0f, (float)ServerOptions.instance.speedLimit.getValue());
            delta *= delta;
            limitMod = GameTime.getInstance().Lerp(1.0f, BaseVehicle.getFakeSpeedModifier(), delta);
        }
        float speedLimited = this.vehicleObject.getCurrentSpeedKmHour() * limitMod;
        this.isGas = false;
        this.isGasR = false;
        this.isBreak = false;
        if (this.clientControls.forward) {
            if (dot < 0.0f) {
                this.isBreak = true;
            }
            if (dot >= 0.0f) {
                this.isGas = true;
            }
            this.isGasR = false;
        }
        if (this.clientControls.backward) {
            if (dot > 0.0f) {
                this.isBreak = true;
            }
            if (dot <= 0.0f) {
                this.isGasR = true;
            }
            this.isGas = false;
        }
        if (this.clientControls.brake) {
            this.isBreak = true;
            this.isGas = false;
            this.isGasR = false;
        }
        if (this.clientControls.forward && this.clientControls.backward) {
            this.isBreak = true;
            this.isGas = false;
            this.isGasR = false;
        }
        if (bDrunkDriver && this.vehicleObject.engineState != BaseVehicle.engineStateTypes.Idle) {
            if (this.isBreak && !this.wasBreaking) {
                this.isBreak = this.delayCommandWhileDrunk(this.isBreak);
            }
            if (this.isGas && !this.wasGas) {
                this.isGas = this.delayCommandWhileDrunk(this.isGas);
            }
            if (this.isGasR && !this.wasGasR) {
                this.isGasR = this.delayCommandWhileDrunk(this.isGas);
            }
            if (this.clientControls.steering != 0.0f && !this.wasSteering) {
                this.clientControls.steering = this.delayCommandWhileDrunk(this.clientControls.steering);
            }
        }
        this.updateRegulator();
        this.wasBreaking = this.isBreak;
        this.wasGas = this.isGas;
        this.wasGasR = this.isGasR;
        boolean bl = this.wasSteering = this.clientControls.steering != 0.0f;
        if (!this.isGasR && this.vehicleObject.isInvalidChunkAhead()) {
            this.isBreak = true;
            this.isGas = false;
            this.isGasR = false;
        } else if (!this.isGas && this.vehicleObject.isInvalidChunkBehind()) {
            this.isBreak = true;
            this.isGas = false;
            this.isGasR = false;
        }
        if (this.clientControls.shift) {
            this.isGas = false;
            this.isBreak = false;
            this.isGasR = false;
            this.clientControls.wasUsingParkingBrakes = false;
        }
        float throttle = this.vehicleObject.throttle;
        throttle = this.isGas || this.isGasR ? (throttle += GameTime.getInstance().getMultiplier() / 30.0f) : (throttle -= GameTime.getInstance().getMultiplier() / 30.0f);
        if (throttle < 0.0f) {
            throttle = 0.0f;
        }
        if (throttle > 1.0f) {
            throttle = 1.0f;
        }
        if (this.vehicleObject.isRegulator() && !this.isGas && !this.isGasR) {
            throttle = 0.5f;
            if (speedLimited < this.vehicleObject.getRegulatorSpeed()) {
                this.isGas = true;
            }
        }
        this.vehicleObject.throttle = throttle;
        float fpsMod = GameTime.getInstance().getMultiplier() / 0.8f;
        ControlState controlState = ControlState.NoControl;
        if (this.isBreak) {
            controlState = ControlState.Braking;
        } else if (this.isGas && !this.isGasR) {
            controlState = ControlState.Forward;
        } else if (!this.isGas && this.isGasR) {
            controlState = ControlState.Reverse;
        }
        if (controlState != ControlState.NoControl) {
            UIManager.speedControls.SetCurrentGameSpeed(1);
        }
        if (controlState == ControlState.NoControl) {
            this.control_NoControl();
        }
        if (controlState == ControlState.Reverse) {
            this.control_Reverse(speedLimited);
        }
        if (controlState == ControlState.Forward) {
            this.control_ForwardNew(speedLimited);
        }
        this.updateBackSignal();
        if (controlState == ControlState.Braking) {
            this.control_Braking();
        }
        this.updateBrakeLights();
        BaseVehicle vehicleTowedBy = this.vehicleObject.getVehicleTowedBy();
        if (vehicleTowedBy != null && vehicleTowedBy.getDriver() == null && this.vehicleObject.getDriver() != null && !GameClient.client) {
            this.vehicleObject.addPointConstraint(null, vehicleTowedBy, this.vehicleObject.getTowAttachmentSelf(), vehicleTowedBy.getTowAttachmentSelf());
        }
        this.updateRammingSound(speedLimited);
        if (Math.abs(this.clientControls.steering) > 0.1f) {
            float delta = 1.0f - this.speed / this.vehicleObject.getMaxSpeed();
            if (delta < 0.1f) {
                delta = 0.1f;
            }
            this.vehicleSteering -= (this.clientControls.steering + this.vehicleSteering) * 0.06f * fpsMod * delta;
        } else if ((double)Math.abs(this.vehicleSteering) <= 0.04) {
            this.vehicleSteering = 0.0f;
        } else if (this.vehicleSteering > 0.0f) {
            this.vehicleSteering -= 0.04f * fpsMod;
            this.vehicleSteering = Math.max(this.vehicleSteering, 0.0f);
        } else {
            this.vehicleSteering += 0.04f * fpsMod;
            this.vehicleSteering = Math.min(this.vehicleSteering, 0.0f);
        }
        float steeringClamp = script.getSteeringClamp(this.speed);
        this.vehicleSteering = PZMath.clamp(this.vehicleSteering, -steeringClamp, steeringClamp);
        BulletVariables bv = bulletVariables.set(this.vehicleObject, this.engineForce, this.brakingForce, this.vehicleSteering);
        this.checkTire(bv);
        this.engineForce = bv.engineForce;
        this.brakingForce = bv.brakingForce;
        this.vehicleSteering = bv.vehicleSteering;
        if (this.vehicleObject.isDoingOffroad()) {
            int tNumb = this.vehicleObject.getTransmissionNumber();
            if (tNumb <= 0) {
                tNumb = 1;
            }
            float mult1 = this.vehicleObject.getVehicleTowing() == null ? 0.6f : 0.8f;
            float mult = PZMath.lerp(0.0f, 1.0f, 1.0f - (float)PZMath.clamp(tNumb - 1, 0, 7) / 15.0f) * mult1 * this.vehicleObject.getScript().getOffroadEfficiency();
            this.engineForce *= mult;
        }
        this.vehicleObject.setCurrentSteering(this.vehicleSteering);
        this.vehicleObject.setBraking(this.isBreak);
        if (!GameServer.server) {
            this.checkShouldBeActive();
            Bullet.controlVehicle(this.vehicleObject.vehicleId, this.engineForce, this.brakingForce, this.vehicleSteering);
            if (this.engineForce > 0.0f && this.vehicleObject.engineState == BaseVehicle.engineStateTypes.Idle && !this.engineStartingFromKeyboard) {
                this.engineStartingFromKeyboard = true;
                if (GameClient.client) {
                    Boolean haveKey = this.vehicleObject.getDriver().getInventory().haveThisKeyId(this.vehicleObject.getKeyId()) != null ? Boolean.TRUE : Boolean.FALSE;
                    GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "startEngine", "haveKey", haveKey);
                } else if (!GameClient.client && !GameServer.server) {
                    Boolean haveKey = this.vehicleObject.getDriver().getInventory().haveThisKeyId(this.vehicleObject.getKeyId()) != null ? Boolean.TRUE : Boolean.FALSE;
                    this.vehicleObject.tryStartEngine(haveKey);
                } else {
                    this.vehicleObject.tryStartEngine();
                }
            }
            if (this.engineStartingFromKeyboard && this.engineForce == 0.0f) {
                this.engineStartingFromKeyboard = false;
            }
        }
        if (this.vehicleObject.engineState != BaseVehicle.engineStateTypes.Running) {
            this.acceleratorOn = false;
            if (!GameServer.server && this.vehicleObject.getCurrentSpeedKmHour() > 5.0f && this.vehicleObject.getScript().getWheelCount() > 0) {
                Bullet.controlVehicle(this.vehicleObject.vehicleId, 0.0f, this.brakingForce, this.vehicleSteering);
            } else {
                this.park();
            }
        }
    }

    public void updateTrailer() {
        BaseVehicle vehicleTowedBy = this.vehicleObject.getVehicleTowedBy();
        if (vehicleTowedBy == null) {
            return;
        }
        if (GameServer.server) {
            if (vehicleTowedBy.getDriver() == null && this.vehicleObject.getDriver() != null) {
                this.vehicleObject.addPointConstraint(null, vehicleTowedBy, this.vehicleObject.getTowAttachmentSelf(), vehicleTowedBy.getTowAttachmentSelf());
            }
            return;
        }
        this.speed = this.vehicleObject.getCurrentSpeedKmHour();
        this.isGas = false;
        this.isGasR = false;
        this.isBreak = false;
        this.wasGas = false;
        this.wasGasR = false;
        this.wasBreaking = false;
        this.vehicleObject.throttle = 0.0f;
        if (vehicleTowedBy.getDriver() == null && this.vehicleObject.getDriver() != null && !GameClient.client) {
            this.vehicleObject.addPointConstraint(null, vehicleTowedBy, this.vehicleObject.getTowAttachmentSelf(), vehicleTowedBy.getTowAttachmentSelf());
            return;
        }
        this.checkShouldBeActive();
        this.engineForce = 0.0f;
        this.brakingForce = 0.0f;
        this.vehicleSteering = 0.0f;
        if (!this.vehicleObject.getScriptName().contains("Trailer")) {
            this.brakingForce = 10.0f;
        }
        Bullet.controlVehicle(this.vehicleObject.vehicleId, this.engineForce, this.brakingForce, this.vehicleSteering);
    }

    private void updateRegulator() {
        if (this.regulatorTimer > 0.0f) {
            this.regulatorTimer -= GameTime.getInstance().getThirtyFPSMultiplier();
        }
        if (this.clientControls.shift) {
            if (this.clientControls.forward && this.regulatorTimer <= 0.0f) {
                if (this.vehicleObject.getRegulatorSpeed() < this.vehicleObject.getMaxSpeed() + 20.0f && (!this.vehicleObject.isRegulator() && this.vehicleObject.getRegulatorSpeed() == 0.0f || this.vehicleObject.isRegulator())) {
                    if (this.vehicleObject.getRegulatorSpeed() == 0.0f && this.vehicleObject.getCurrentSpeedForRegulator() != this.vehicleObject.getRegulatorSpeed()) {
                        this.vehicleObject.setRegulatorSpeed(this.vehicleObject.getCurrentSpeedForRegulator());
                    } else {
                        this.vehicleObject.setRegulatorSpeed(this.vehicleObject.getRegulatorSpeed() + 5.0f);
                    }
                }
                this.vehicleObject.setRegulator(true);
                this.regulatorTimer = 20.0f;
            } else if (this.clientControls.backward && this.regulatorTimer <= 0.0f) {
                this.regulatorTimer = 20.0f;
                if (this.vehicleObject.getRegulatorSpeed() >= 5.0f && (!this.vehicleObject.isRegulator() && this.vehicleObject.getRegulatorSpeed() == 0.0f || this.vehicleObject.isRegulator())) {
                    this.vehicleObject.setRegulatorSpeed(this.vehicleObject.getRegulatorSpeed() - 5.0f);
                }
                this.vehicleObject.setRegulator(true);
                if (this.vehicleObject.getRegulatorSpeed() <= 0.0f) {
                    this.vehicleObject.setRegulatorSpeed(0.0f);
                    this.vehicleObject.setRegulator(false);
                }
            }
        } else if (this.isGasR || this.isBreak) {
            this.vehicleObject.setRegulator(false);
        }
    }

    public float getVehicleSteering() {
        return this.vehicleSteering;
    }

    public boolean isGas() {
        return this.isGas;
    }

    public boolean isGasR() {
        return this.isGasR;
    }

    public boolean isBreak() {
        return this.isBreak;
    }

    public void control_NoControl() {
        float fpsMult = GameTime.getInstance().getMultiplier() / 0.8f;
        if (!this.vehicleObject.isEngineRunning()) {
            if (this.vehicleObject.engineSpeed > 0.0) {
                this.vehicleObject.engineSpeed = Math.max(this.vehicleObject.engineSpeed - (double)(50.0f * fpsMult), 0.0);
            }
        } else if (this.vehicleObject.engineSpeed > (double)this.vehicleObject.getScript().getEngineIdleSpeed()) {
            if (!this.vehicleObject.isRegulator()) {
                this.vehicleObject.engineSpeed -= (double)(20.0f * fpsMult);
            }
        } else {
            this.vehicleObject.engineSpeed += (double)(20.0f * fpsMult);
        }
        if (!this.vehicleObject.isRegulator()) {
            this.vehicleObject.transmissionNumber = TransmissionNumber.N;
        }
        this.engineForce = 0.0f;
        this.brakingForce = this.vehicleObject.engineSpeed > 1000.0 ? 15.0f : 10.0f;
    }

    private void control_Braking() {
        float fpsMult = GameTime.getInstance().getMultiplier() / 0.8f;
        this.vehicleObject.engineSpeed = this.vehicleObject.engineSpeed > (double)this.vehicleObject.getScript().getEngineIdleSpeed() ? (this.vehicleObject.engineSpeed -= (double)((float)Rand.Next(10, 30) * fpsMult)) : (this.vehicleObject.engineSpeed += (double)((float)Rand.Next(20) * fpsMult));
        this.vehicleObject.transmissionNumber = TransmissionNumber.N;
        this.engineForce = 0.0f;
        this.brakingForce = this.vehicleObject.getBrakingForce();
        if (this.clientControls.brake) {
            this.brakingForce *= 13.0f;
        }
    }

    private void control_Forward(float speed) {
        float fpsMult = GameTime.getInstance().getMultiplier() / 0.8f;
        IsoGameCharacter driver = this.vehicleObject.getDriver();
        boolean trainFastDriver = driver != null && driver.hasTrait(CharacterTrait.SPEED_DEMON);
        boolean trainSlowDriver = driver != null && driver.hasTrait(CharacterTrait.SUNDAY_DRIVER);
        int gearRatioCount = this.vehicleObject.getScript().gearRatioCount;
        float engineSpeedCalc = 0.0f;
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.N) {
            this.vehicleObject.transmissionNumber = TransmissionNumber.Speed1;
            boolean isChangeTransmission = false;
            while (true) {
                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1) {
                    engineSpeedCalc = 3000.0f * speed / 30.0f;
                }
                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed2) {
                    engineSpeedCalc = 3000.0f * speed / 40.0f;
                }
                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed3) {
                    engineSpeedCalc = 3000.0f * speed / 60.0f;
                }
                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed4) {
                    engineSpeedCalc = 3000.0f * speed / 85.0f;
                }
                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed5) {
                    engineSpeedCalc = 3000.0f * speed / 105.0f;
                }
                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed6) {
                    engineSpeedCalc = 3000.0f * speed / 130.0f;
                }
                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed7) {
                    engineSpeedCalc = 3000.0f * speed / 160.0f;
                }
                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed8) {
                    engineSpeedCalc = 3000.0f * speed / 200.0f;
                }
                if (engineSpeedCalc > 3000.0f) {
                    this.vehicleObject.changeTransmission(this.vehicleObject.transmissionNumber.getNext(gearRatioCount));
                    isChangeTransmission = true;
                }
                if (!isChangeTransmission || this.vehicleObject.transmissionNumber.getIndex() >= gearRatioCount) break;
                isChangeTransmission = false;
            }
        }
        if (this.vehicleObject.engineSpeed > 3000.0 && this.vehicleObject.transmissionChangeTime.Check()) {
            this.vehicleObject.changeTransmission(this.vehicleObject.transmissionNumber.getNext(gearRatioCount));
        }
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1) {
            engineSpeedCalc = 3000.0f * speed / 30.0f;
        }
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed2) {
            engineSpeedCalc = 3000.0f * speed / 40.0f;
        }
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed3) {
            engineSpeedCalc = 3000.0f * speed / 60.0f;
        }
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed4) {
            engineSpeedCalc = 3000.0f * speed / 85.0f;
        }
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed5) {
            engineSpeedCalc = 3000.0f * speed / 105.0f;
        }
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed6) {
            engineSpeedCalc = 3000.0f * speed / 130.0f;
        }
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed7) {
            engineSpeedCalc = 3000.0f * speed / 160.0f;
        }
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed8) {
            engineSpeedCalc = 3000.0f * speed / 200.0f;
        }
        this.vehicleObject.engineSpeed -= Math.min(0.5 * (this.vehicleObject.engineSpeed - (double)engineSpeedCalc), 100.0) * (double)fpsMult;
        if (trainFastDriver) {
            if (speed < 50.0f) {
                this.vehicleObject.engineSpeed -= Math.min(0.06 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0f - speed)) * (double)fpsMult;
            }
        } else if (speed < 30.0f) {
            this.vehicleObject.engineSpeed -= Math.min(0.02 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0f - speed)) * (double)fpsMult;
        }
        this.engineForce = (float)((double)this.vehicleObject.getEnginePower() * (0.5 + this.vehicleObject.engineSpeed / 24000.0));
        this.engineForce -= this.engineForce * (speed / 200.0f);
        boolean towingBurntVehicle = false;
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1 && this.vehicleObject.getVehicleTowedBy() != null) {
            if (this.vehicleObject.getVehicleTowedBy().getScript().getPassengerCount() == 0 && this.vehicleObject.getVehicleTowedBy().getScript().getMass() > 200.0f) {
                towingBurntVehicle = true;
            }
            int n = towingBurntVehicle ? 20 : 5;
            if (speed < (float)n) {
                this.engineForce *= Math.min(1.2f, this.vehicleObject.getVehicleTowedBy().getMass() / 500.0f);
                if (towingBurntVehicle) {
                    this.engineForce *= 4.0f;
                }
            }
        }
        if (this.vehicleObject.engineSpeed > 6000.0) {
            this.engineForce = (float)((double)this.engineForce * ((7000.0 - this.vehicleObject.engineSpeed) / 1000.0));
        }
        if (trainSlowDriver) {
            this.engineForce *= 0.75f;
            if (speed > this.vehicleObject.getMaxSpeed() * 0.6f) {
                this.engineForce *= (this.vehicleObject.getMaxSpeed() * 0.75f + 20.0f - speed) / 20.0f;
            }
        }
        if (trainFastDriver) {
            if (speed > this.vehicleObject.getMaxSpeed() * 1.15f) {
                this.engineForce *= (this.vehicleObject.getMaxSpeed() * 1.15f + 20.0f - speed) / 20.0f;
            }
        } else if (speed > this.vehicleObject.getMaxSpeed()) {
            this.engineForce *= (this.vehicleObject.getMaxSpeed() + 20.0f - speed) / 20.0f;
        }
        this.brakingForce = 0.0f;
        if (this.clientControls.wasUsingParkingBrakes) {
            this.clientControls.wasUsingParkingBrakes = false;
            this.engineForce *= 8.0f;
        }
        if (GameClient.client && (double)this.vehicleObject.getCurrentSpeedKmHour() >= ServerOptions.instance.speedLimit.getValue()) {
            this.engineForce = 0.0f;
        }
    }

    private void control_ForwardNew(float speed) {
        float fpsMult = GameTime.getInstance().getMultiplier() / 0.8f;
        IsoGameCharacter driver = this.vehicleObject.getDriver();
        boolean trainFastDriver = driver != null && driver.hasTrait(CharacterTrait.SPEED_DEMON);
        boolean trainSlowDriver = driver != null && driver.hasTrait(CharacterTrait.SUNDAY_DRIVER);
        int gearRatioCount = this.vehicleObject.getScript().gearRatioCount;
        EngineRPMData[] carRpmData = this.vehicleObject.getVehicleEngineRPM().rpmData;
        float speedPerGear = this.vehicleObject.getMaxSpeed() / (float)gearRatioCount;
        float speedClamped = PZMath.clamp(speed, 0.0f, this.vehicleObject.getMaxSpeed());
        int gearForSpeed = (int)PZMath.floor(speedClamped / speedPerGear) + 1;
        gearForSpeed = PZMath.min(gearForSpeed, gearRatioCount);
        float engineSpeedCalc = carRpmData[gearForSpeed - 1].gearChange;
        TransmissionNumber targetTransmissionNumber = TransmissionNumber.Speed1;
        switch (gearForSpeed) {
            case 1: {
                targetTransmissionNumber = TransmissionNumber.Speed1;
                break;
            }
            case 2: {
                targetTransmissionNumber = TransmissionNumber.Speed2;
                break;
            }
            case 3: {
                targetTransmissionNumber = TransmissionNumber.Speed3;
                break;
            }
            case 4: {
                targetTransmissionNumber = TransmissionNumber.Speed4;
                break;
            }
            case 5: {
                targetTransmissionNumber = TransmissionNumber.Speed5;
                break;
            }
            case 6: {
                targetTransmissionNumber = TransmissionNumber.Speed6;
                break;
            }
            case 7: {
                targetTransmissionNumber = TransmissionNumber.Speed7;
                break;
            }
            case 8: {
                targetTransmissionNumber = TransmissionNumber.Speed8;
            }
        }
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.N) {
            this.vehicleObject.transmissionNumber = targetTransmissionNumber;
        } else if (this.vehicleObject.transmissionNumber.getIndex() - 1 >= 0 && this.vehicleObject.transmissionNumber.getIndex() < targetTransmissionNumber.getIndex() && this.vehicleObject.getEngineSpeed() >= (double)carRpmData[this.vehicleObject.transmissionNumber.getIndex() - 1].gearChange && speed >= speedPerGear * (float)this.vehicleObject.transmissionNumber.getIndex()) {
            this.vehicleObject.transmissionNumber = targetTransmissionNumber;
            this.vehicleObject.engineSpeed = carRpmData[this.vehicleObject.transmissionNumber.getIndex() - 1].afterGearChange;
        }
        if (this.vehicleObject.transmissionNumber.getIndex() < gearRatioCount && this.vehicleObject.transmissionNumber.getIndex() - 1 >= 0) {
            this.vehicleObject.engineSpeed = Math.min(this.vehicleObject.engineSpeed, (double)(carRpmData[this.vehicleObject.transmissionNumber.getIndex() - 1].gearChange + 100.0f));
        }
        if (this.vehicleObject.engineSpeed > (double)engineSpeedCalc) {
            this.vehicleObject.engineSpeed -= Math.min(0.5 * (this.vehicleObject.engineSpeed - (double)engineSpeedCalc), 10.0) * (double)fpsMult;
        } else {
            float rpmIncrease = switch (this.vehicleObject.transmissionNumber) {
                case TransmissionNumber.Speed1 -> 10.0f;
                case TransmissionNumber.Speed2 -> 8.0f;
                case TransmissionNumber.Speed3 -> 7.0f;
                case TransmissionNumber.Speed4 -> 6.0f;
                case TransmissionNumber.Speed5 -> 5.0f;
                default -> 4.0f;
            };
            this.vehicleObject.engineSpeed += (double)(rpmIncrease * fpsMult);
        }
        float enginePower = this.vehicleObject.getScript().getEngineForce();
        this.engineForce = (float)((double)(enginePower *= (switch (this.vehicleObject.transmissionNumber) {
            case TransmissionNumber.Speed1 -> 1.5f;
            default -> 1.0f;
        })) * ((double)0.3f + this.vehicleObject.engineSpeed / 30000.0));
        this.engineForce -= this.engineForce * (speed / 200.0f);
        boolean towingBurntVehicle = false;
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1 && this.vehicleObject.getVehicleTowedBy() != null) {
            if (this.vehicleObject.getVehicleTowedBy().getScript().getPassengerCount() == 0 && this.vehicleObject.getVehicleTowedBy().getScript().getMass() > 200.0f) {
                towingBurntVehicle = true;
            }
            int n = towingBurntVehicle ? 20 : 5;
            if (speed < (float)n) {
                this.engineForce *= Math.min(1.2f, this.vehicleObject.getVehicleTowedBy().getMass() / 500.0f);
                if (towingBurntVehicle) {
                    this.engineForce *= 4.0f;
                }
            }
        }
        if (this.vehicleObject.engineSpeed > 6000.0) {
            this.engineForce = (float)((double)this.engineForce * ((7000.0 - this.vehicleObject.engineSpeed) / 1000.0));
        }
        if (trainSlowDriver) {
            this.engineForce *= 0.75f;
            if (speed > this.vehicleObject.getMaxSpeed() * 0.6f) {
                this.engineForce *= (this.vehicleObject.getMaxSpeed() * 0.75f + 20.0f - speed) / 20.0f;
            }
        }
        if (trainFastDriver) {
            if (speed > this.vehicleObject.getMaxSpeed() * 1.15f) {
                this.engineForce *= (this.vehicleObject.getMaxSpeed() * 1.15f + 20.0f - speed) / 20.0f;
            }
        } else if (speed > this.vehicleObject.getMaxSpeed()) {
            this.engineForce *= (this.vehicleObject.getMaxSpeed() + 20.0f - speed) / 20.0f;
        }
        this.brakingForce = 0.0f;
        if (this.clientControls.wasUsingParkingBrakes) {
            this.clientControls.wasUsingParkingBrakes = false;
            this.engineForce *= 8.0f;
        }
        if (GameClient.client && (double)this.vehicleObject.getCurrentSpeedKmHour() >= ServerOptions.instance.speedLimit.getValue()) {
            this.engineForce = 0.0f;
        }
    }

    private void control_Reverse(float speed) {
        float fpsMult = GameTime.getInstance().getMultiplier() / 0.8f;
        IsoGameCharacter driver = this.vehicleObject.getDriver();
        boolean trainFastDriver = driver != null && driver.hasTrait(CharacterTrait.SPEED_DEMON);
        boolean trainSlowDriver = driver != null && driver.hasTrait(CharacterTrait.SUNDAY_DRIVER);
        this.vehicleObject.transmissionNumber = TransmissionNumber.R;
        float engineSpeedCalc = 1000.0f * (speed *= 1.5f) / 30.0f;
        this.vehicleObject.engineSpeed -= Math.min(0.5 * (this.vehicleObject.engineSpeed - (double)engineSpeedCalc), 100.0) * (double)fpsMult;
        this.vehicleObject.engineSpeed = trainFastDriver ? (this.vehicleObject.engineSpeed -= Math.min(0.06 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0f - speed)) * (double)fpsMult) : (this.vehicleObject.engineSpeed -= Math.min(0.02 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0f - speed)) * (double)fpsMult);
        this.engineForce = (float)((double)(-1.0f * (float)this.vehicleObject.getEnginePower()) * (0.75 + this.vehicleObject.engineSpeed / 24000.0));
        if (this.vehicleObject.engineSpeed > 6000.0) {
            this.engineForce = (float)((double)this.engineForce * ((7000.0 - this.vehicleObject.engineSpeed) / 1000.0));
        }
        if (trainSlowDriver) {
            this.engineForce *= 0.7f;
            if (speed < -5.0f) {
                this.engineForce *= (15.0f + speed) / 10.0f;
            }
        }
        if (speed < -1.0f * this.vehicleObject.getScript().maxSpeedReverse) {
            this.engineForce = 0.0f;
        }
        this.brakingForce = 0.0f;
    }

    private void updateRammingSound(float speed) {
        if (this.vehicleObject.isEngineRunning() && (speed < 1.0f && this.engineForce > this.vehicleObject.getScript().getEngineIdleSpeed() * 2.0f || speed > -0.5f && this.engineForce < this.vehicleObject.getScript().getEngineIdleSpeed() * -2.0f)) {
            if (this.vehicleObject.ramSound == 0L) {
                this.vehicleObject.ramSound = this.vehicleObject.playSoundImpl("VehicleSkid", null);
                this.vehicleObject.ramSoundTime = System.currentTimeMillis() + 1000L + (long)Rand.Next(2000);
            }
            if (this.vehicleObject.ramSound != 0L && this.vehicleObject.ramSoundTime < System.currentTimeMillis()) {
                this.vehicleObject.stopSound(this.vehicleObject.ramSound);
                this.vehicleObject.ramSound = 0L;
            }
        } else if (this.vehicleObject.ramSound != 0L) {
            this.vehicleObject.stopSound(this.vehicleObject.ramSound);
            this.vehicleObject.ramSound = 0L;
        }
    }

    private void updateBackSignal() {
        if (this.isGasR && this.vehicleObject.isEngineRunning() && this.vehicleObject.hasBackSignal() && !this.vehicleObject.isBackSignalEmitting()) {
            if (GameClient.client) {
                GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "onBackSignal", "state", "start");
            } else {
                this.vehicleObject.onBackMoveSignalStart();
            }
        }
        if (!this.isGasR && this.vehicleObject.isBackSignalEmitting()) {
            if (GameClient.client) {
                GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "onBackSignal", "state", "stop");
            } else {
                this.vehicleObject.onBackMoveSignalStop();
            }
        }
    }

    private void updateBrakeLights() {
        if (this.isBreak) {
            if (this.vehicleObject.getStoplightsOn()) {
                return;
            }
            if (GameClient.client) {
                GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "setStoplightsOn", "on", Boolean.TRUE);
            }
            if (!GameServer.server) {
                this.vehicleObject.setStoplightsOn(true);
            }
        } else {
            if (!this.vehicleObject.getStoplightsOn()) {
                return;
            }
            if (GameClient.client) {
                GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "setStoplightsOn", "on", Boolean.FALSE);
            }
            if (!GameServer.server) {
                this.vehicleObject.setStoplightsOn(false);
            }
        }
    }

    private boolean delayCommandWhileDrunk(boolean command) {
        this.drunkDelayCommandTimer += GameTime.getInstance().getMultiplier();
        if ((float)Rand.AdjustForFramerate(4 * this.vehicleObject.getDriver().getMoodles().getMoodleLevel(MoodleType.DRUNK)) < this.drunkDelayCommandTimer) {
            this.drunkDelayCommandTimer = 0.0f;
            return true;
        }
        return false;
    }

    private float delayCommandWhileDrunk(float steering) {
        this.drunkDelayCommandTimer += GameTime.getInstance().getMultiplier();
        if ((float)Rand.AdjustForFramerate(4 * this.vehicleObject.getDriver().getMoodles().getMoodleLevel(MoodleType.DRUNK)) < this.drunkDelayCommandTimer) {
            this.drunkDelayCommandTimer = 0.0f;
            return steering;
        }
        return 0.0f;
    }

    private void checkTire(BulletVariables bv) {
        if (this.vehicleObject.getPartById("TireFrontLeft") == null || this.vehicleObject.getPartById("TireFrontLeft").getInventoryItem() == null) {
            bv.brakingForce = (float)((double)bv.brakingForce / 1.2);
            bv.engineForce = (float)((double)bv.engineForce / 1.2);
        }
        if (this.vehicleObject.getPartById("TireFrontRight") == null || this.vehicleObject.getPartById("TireFrontRight").getInventoryItem() == null) {
            bv.brakingForce = (float)((double)bv.brakingForce / 1.2);
            bv.engineForce = (float)((double)bv.engineForce / 1.2);
        }
        if (this.vehicleObject.getPartById("TireRearLeft") == null || this.vehicleObject.getPartById("TireRearLeft").getInventoryItem() == null) {
            bv.brakingForce = (float)((double)bv.brakingForce / 1.3);
            bv.engineForce = (float)((double)bv.engineForce / 1.3);
        }
        if (this.vehicleObject.getPartById("TireRearRight") == null || this.vehicleObject.getPartById("TireRearRight").getInventoryItem() == null) {
            bv.brakingForce = (float)((double)bv.brakingForce / 1.3);
            bv.engineForce = (float)((double)bv.engineForce / 1.3);
        }
    }

    public void updateControls() {
        if (!GameServer.server) {
            long dt;
            int joypad;
            if (this.vehicleObject.isKeyboardControlled()) {
                boolean left = GameKeyboard.isKeyDown("Left");
                boolean right = GameKeyboard.isKeyDown("Right");
                boolean forward = GameKeyboard.isKeyDown("Forward");
                boolean backward = GameKeyboard.isKeyDown("Backward");
                boolean brake = GameKeyboard.isKeyDown("Brake");
                boolean shift = GameKeyboard.isKeyDown("CruiseControl");
                this.clientControls.steering = 0.0f;
                if (left) {
                    this.clientControls.steering -= 1.0f;
                }
                if (right) {
                    this.clientControls.steering += 1.0f;
                }
                this.clientControls.forward = forward;
                this.clientControls.backward = backward;
                this.clientControls.brake = brake;
                this.clientControls.shift = shift;
                if (this.clientControls.brake) {
                    this.clientControls.wasUsingParkingBrakes = true;
                }
            }
            if ((joypad = this.vehicleObject.getJoypad()) != -1) {
                boolean left = JoypadManager.instance.isLeftPressed(joypad);
                boolean right = JoypadManager.instance.isRightPressed(joypad);
                boolean forward = JoypadManager.instance.isRTPressed(joypad);
                boolean backward = JoypadManager.instance.isLTPressed(joypad);
                boolean brake = JoypadManager.instance.isBPressed(joypad);
                this.clientControls.steering = JoypadManager.instance.getMovementAxisX(joypad);
                this.clientControls.forward = forward;
                this.clientControls.backward = backward;
                this.clientControls.brake = brake;
            }
            if (this.clientControls.forceBrake != 0L && (dt = System.currentTimeMillis() - this.clientControls.forceBrake) > 0L && dt < 1000L) {
                this.clientControls.brake = true;
                this.clientControls.shift = false;
            }
        }
    }

    public void park() {
        if (!GameServer.server && this.vehicleObject.getScript().getWheelCount() > 0) {
            Bullet.controlVehicle(this.vehicleObject.vehicleId, 0.0f, this.vehicleObject.getBrakingForce(), 0.0f);
        }
        this.wasGas = false;
        this.isGas = false;
        this.wasGasR = false;
        this.isGasR = false;
        this.clientControls.reset();
        this.vehicleObject.transmissionNumber = TransmissionNumber.N;
        if (this.vehicleObject.getVehicleTowing() != null) {
            this.vehicleObject.getVehicleTowing().getController().park();
        }
    }

    protected boolean shouldBeActive() {
        if (this.vehicleObject.physicActiveCheck != -1L) {
            return true;
        }
        if (this.isPlayerDrivenVehicleNearby()) {
            return true;
        }
        BaseVehicle vehicleTowedBy = this.vehicleObject.getVehicleTowedBy();
        if (vehicleTowedBy == null) {
            float engineForce = this.vehicleObject.isEngineRunning() ? this.engineForce : 0.0f;
            return Math.abs(engineForce) > 0.01f;
        }
        if (vehicleTowedBy.getController() == null) {
            return false;
        }
        return vehicleTowedBy.getController().shouldBeActive();
    }

    public void checkShouldBeActive() {
        if (this.shouldBeActive()) {
            if (!this.isEnable) {
                this.vehicleObject.setPhysicsActive(true);
            }
            this.atRestTimer = 1.0f;
        } else if (this.isEnable && this.vehicleObject.isAtRest()) {
            if (this.atRestTimer > 0.0f) {
                this.atRestTimer -= GameTime.getInstance().getTimeDelta();
            }
            if (this.atRestTimer <= 0.0f) {
                this.vehicleObject.setPhysicsActive(false);
            }
        }
    }

    public boolean isGasPedalPressed() {
        return this.isGas || this.isGasR;
    }

    public boolean isBrakePedalPressed() {
        return this.isBreak;
    }

    private BaseVehicle getPlayerDrivenVehicleNearby() {
        int chunkX = PZMath.coorddivision(this.vehicleObject.getXi(), 8);
        int chunkY = PZMath.coorddivision(this.vehicleObject.getYi(), 8);
        Vector2f p1 = BaseVehicle.allocVector2f();
        Vector2f p2 = BaseVehicle.allocVector2f();
        BaseVehicle closest = null;
        float closestDistSq = Float.MAX_VALUE;
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunk(chunkX + dx, chunkY + dy);
                if (chunk == null) continue;
                for (BaseVehicle vehicle : chunk.vehicles) {
                    float distSq;
                    if (vehicle == this.vehicleObject || vehicle.getDriver() == null || !((distSq = this.vehicleObject.getClosestPointOnPoly(vehicle, p1, p2)) < 9.0f) || !(distSq < closestDistSq)) continue;
                    closest = vehicle;
                    closestDistSq = distSq;
                }
            }
        }
        BaseVehicle.releaseVector2f(p1);
        BaseVehicle.releaseVector2f(p2);
        return closest;
    }

    private boolean isPlayerDrivenVehicleNearby() {
        return this.getPlayerDrivenVehicleNearby() != null;
    }

    public void debug() {
        Vector2 vecAim;
        int joypad;
        if (!Core.debug || !DebugOptions.instance.vehicleRenderOutline.getValue()) {
            return;
        }
        VehicleScript script = this.vehicleObject.getScript();
        int zi = PZMath.fastfloor(this.vehicleObject.getZ());
        Vector3f vec = this.tempVec3f;
        this.vehicleObject.getForwardVector(vec);
        this.vehicleObject.getWorldTransform(this.tempXfrm);
        VehiclePoly poly = this.vehicleObject.getPoly();
        LineDrawer.addLine(poly.x1, poly.y1, zi, poly.x2, poly.y2, zi, 1.0f, 1.0f, 1.0f, null, true);
        LineDrawer.addLine(poly.x2, poly.y2, zi, poly.x3, poly.y3, zi, 1.0f, 1.0f, 1.0f, null, true);
        LineDrawer.addLine(poly.x3, poly.y3, zi, poly.x4, poly.y4, zi, 1.0f, 1.0f, 1.0f, null, true);
        LineDrawer.addLine(poly.x4, poly.y4, zi, poly.x1, poly.y1, zi, 1.0f, 1.0f, 1.0f, null, true);
        Vector2f closestPos = BaseVehicle.allocVector2f();
        float px = IsoCamera.frameState.camCharacterX;
        float py = IsoCamera.frameState.camCharacterY;
        this.vehicleObject.getClosestPointOnPoly(px, py, closestPos);
        if (this.vehicleObject.isPointLeftOfCenter(closestPos.x, closestPos.y)) {
            this.drawCircle(closestPos.x, closestPos.y, 0.05f, 0.0f, 1.0f, 0.0f, 1.0f);
        } else {
            this.drawCircle(closestPos.x, closestPos.y, 0.05f, 0.0f, 0.0f, 1.0f, 1.0f);
        }
        BaseVehicle.releaseVector2f(closestPos);
        _UNIT_Y.set(0.0f, 1.0f, 0.0f);
        for (int i = 0; i < this.vehicleObject.getScript().getWheelCount(); ++i) {
            VehicleScript.Wheel scriptWheel = script.getWheel(i);
            this.tempVec3f.set(scriptWheel.getOffset());
            if (script.getModel() != null) {
                this.tempVec3f.add(script.getModelOffset());
            }
            this.vehicleObject.getWorldPos(this.tempVec3f, this.tempVec3f);
            float originX = this.tempVec3f.x;
            float originY = this.tempVec3f.y;
            this.vehicleObject.getWheelForwardVector(i, this.tempVec3f);
            LineDrawer.addLine(originX, originY, zi, originX + this.tempVec3f.x, originY + this.tempVec3f.z, zi, 1.0f, 1.0f, 1.0f, null, true);
            this.drawRect(this.tempVec3f, originX - WorldSimulation.instance.offsetX, originY - WorldSimulation.instance.offsetY, scriptWheel.width, scriptWheel.radius);
        }
        if (this.vehicleObject.collideX != -1.0f) {
            this.vehicleObject.getForwardVector(vec);
            this.drawCircle(this.vehicleObject.collideX, this.vehicleObject.collideY, 0.3f);
            this.vehicleObject.collideX = -1.0f;
            this.vehicleObject.collideY = -1.0f;
        }
        if ((joypad = this.vehicleObject.getJoypad()) != -1 && (vecAim = JoypadManager.instance.getMovementAxis(joypad, this.tempVec2)).getLengthSquared() > 1.0E-4f) {
            vecAim.setLength(4.0f);
            vecAim.rotate(-0.7853982f);
            LineDrawer.addLine(this.vehicleObject.getX(), this.vehicleObject.getY(), this.vehicleObject.getZ(), this.vehicleObject.getX() + vecAim.x, this.vehicleObject.getY() + vecAim.y, this.vehicleObject.getZ(), 1.0f, 1.0f, 1.0f, null, true);
        }
        float x = this.vehicleObject.getX();
        float y = this.vehicleObject.getY();
        float z = this.vehicleObject.getZ();
        LineDrawer.addLine(x - 0.5f, y, z, x + 0.5f, y, z, 1.0f, 1.0f, 1.0f, null, true);
        LineDrawer.addLine(x, y - 0.5f, z, x, y + 0.5f, z, 1.0f, 1.0f, 1.0f, null, true);
        this.renderClosestPointToOtherVehicle();
    }

    private void renderClosestPointToOtherVehicle() {
        Vector2f p2;
        ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();
        BaseVehicle closest = null;
        float closestDistSq = Float.MAX_VALUE;
        for (int i = 0; i < vehicles.size(); ++i) {
            float distSq;
            BaseVehicle other = vehicles.get(i);
            if (other == this.vehicleObject || !((distSq = IsoUtils.DistanceToSquared(this.vehicleObject.getX(), this.vehicleObject.getY(), other.getX(), other.getY())) < closestDistSq)) continue;
            closestDistSq = distSq;
            closest = other;
        }
        if (closest == null || closestDistSq > 100.0f) {
            return;
        }
        Vector2f p1 = BaseVehicle.allocVector2f();
        closestDistSq = this.vehicleObject.getClosestPointOnPoly(closest, p1, p2 = BaseVehicle.allocVector2f());
        if (closestDistSq == 0.0f) {
            LineDrawer.addRect(p1.x, p1.y, this.vehicleObject.getZ(), 0.05f, 0.05f, 0.0f, 1.0f, 1.0f);
        } else {
            LineDrawer.addLine(p1.x, p1.y, this.vehicleObject.getZ(), p2.x, p2.y, closest.getZ(), 0.0f, 1.0f, 1.0f, 1.0f);
        }
        BaseVehicle.releaseVector2f(p1);
        BaseVehicle.releaseVector2f(p2);
    }

    public void drawRect(Vector3f vec, float x, float y, float w, float h) {
        this.drawRect(vec, x, y, w, h, 1.0f, 1.0f, 1.0f);
    }

    public void drawRect(Vector3f vec, float x, float y, float w, float h, float r, float g, float b) {
        float vecX = vec.x;
        float vecY = vec.y;
        float vecZ = vec.z;
        Vector3f vec2 = this.tempVec3f3;
        vec.cross(_UNIT_Y, vec2);
        float mul = 1.0f;
        vec.x *= 1.0f * h;
        vec.z *= 1.0f * h;
        vec2.x *= 1.0f * w;
        vec2.z *= 1.0f * w;
        float fx = x + vec.x;
        float fy = y + vec.z;
        float bx = x - vec.x;
        float by = y - vec.z;
        float fx1 = fx - vec2.x / 2.0f;
        float fx2 = fx + vec2.x / 2.0f;
        float bx1 = bx - vec2.x / 2.0f;
        float bx2 = bx + vec2.x / 2.0f;
        float by1 = by - vec2.z / 2.0f;
        float by2 = by + vec2.z / 2.0f;
        float fy1 = fy - vec2.z / 2.0f;
        float fy2 = fy + vec2.z / 2.0f;
        fx1 += WorldSimulation.instance.offsetX;
        fy1 += WorldSimulation.instance.offsetY;
        fx2 += WorldSimulation.instance.offsetX;
        fy2 += WorldSimulation.instance.offsetY;
        bx1 += WorldSimulation.instance.offsetX;
        int z = PZMath.fastfloor(this.vehicleObject.getZ());
        float a = this.vehicleObject.getAlpha(IsoPlayer.getPlayerIndex());
        LineDrawer.addLine(fx1, fy1, (float)z, fx2, fy2, (float)z, r, g, b, a);
        LineDrawer.addLine(fx1, fy1, (float)z, bx1, by1 += WorldSimulation.instance.offsetY, (float)z, r, g, b, a);
        LineDrawer.addLine(fx2, fy2, (float)z, bx2 += WorldSimulation.instance.offsetX, by2 += WorldSimulation.instance.offsetY, (float)z, r, g, b, a);
        LineDrawer.addLine(bx1, by1, (float)z, bx2, by2, (float)z, r, g, b, a);
        vec.set(vecX, vecY, vecZ);
    }

    public void drawCircle(float x, float y, float radius) {
        this.drawCircle(x, y, radius, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void drawCircle(float x, float y, float radius, float r, float g, float b, float a) {
        LineDrawer.DrawIsoCircle(x, y, this.vehicleObject.getZ(), radius, 16, r, g, b, a);
    }

    static {
        CarController.gears[0] = new GearInfo(0, 25, 0.0f);
        CarController.gears[1] = new GearInfo(25, 50, 0.5f);
        CarController.gears[2] = new GearInfo(50, 1000, 0.5f);
        bulletVariables = new BulletVariables();
    }

    public static final class ClientControls {
        public float steering;
        public boolean forward;
        public boolean backward;
        public boolean brake;
        public boolean shift;
        public boolean wasUsingParkingBrakes;
        public long forceBrake;

        public void reset() {
            this.steering = 0.0f;
            this.forward = false;
            this.backward = false;
            this.brake = false;
            this.shift = false;
            this.wasUsingParkingBrakes = false;
            this.forceBrake = 0L;
        }
    }

    public static final class GearInfo {
        private final int minSpeed;
        private final int maxSpeed;
        private final float minRpm;

        private GearInfo(int minSpeed, int maxSpeed, float rpm) {
            this.minSpeed = minSpeed;
            this.maxSpeed = maxSpeed;
            this.minRpm = rpm;
        }
    }

    static enum ControlState {
        NoControl,
        Braking,
        Forward,
        Reverse;

    }

    public static final class BulletVariables {
        private float engineForce;
        private float brakingForce;
        private float vehicleSteering;
        private BaseVehicle vehicle;

        private BulletVariables set(BaseVehicle vehicle, float engineForce, float brakingForce, float vehicleSteering) {
            this.vehicle = vehicle;
            this.engineForce = engineForce;
            this.brakingForce = brakingForce;
            this.vehicleSteering = vehicleSteering;
            return this;
        }
    }
}

